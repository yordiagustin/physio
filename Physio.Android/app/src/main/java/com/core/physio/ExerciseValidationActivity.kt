package com.core.physio

import PoseAnalyzer
import PoseOverlay
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.core.physio.ui.theme.PhysioTheme
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ExerciseValidationActivity : ComponentActivity() {
    private var hasCameraPermission by mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            hasCameraPermission = isGranted
        }

    private lateinit var backgroundExecutor: ExecutorService

    private var poseLandmarker: PoseLandmarker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        checkAndRequestCameraPermission()

        backgroundExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

        setContent {
            PhysioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasCameraPermission) {
                        Log.d(
                            "ExerciseValidationActivity",
                            "Camera permission granted, showing PoseEstimationScreen."
                        )
                        PoseEstimationScreen(
                            executor = backgroundExecutor,
                            onLandmarkerInitialized = { landmarker ->
                                this.poseLandmarker = landmarker
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            Log.d(
                                "ExerciseValidationActivity",
                                "Camera permission not granted, requesting."
                            )
                            checkAndRequestCameraPermission()
                        }
                        Log.d("ExerciseValidationActivity", "Waiting for camera permission...")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::backgroundExecutor.isInitialized) {
            backgroundExecutor.shutdown()
        }

        poseLandmarker?.close()
        Log.d("ExerciseValidationActivity", "Activity destroyed, PoseLandmarker closed.")
    }

    private fun checkAndRequestCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) -> {
                hasCameraPermission = true
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    @Composable
    fun PoseEstimationScreen(
        executor: ExecutorService,
        onLandmarkerInitialized: (PoseLandmarker?) -> Unit
    ) {
        Log.d("PoseEstimationScreen", "PoseEstimationScreen Composable called/recomposed.")

        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val lensFacing = CameraSelector.LENS_FACING_BACK

        var poseResults by remember { mutableStateOf<PoseLandmarkerResult?>(null) }
        var inputImageWidth by remember { mutableStateOf(1) }
        var inputImageHeight by remember { mutableStateOf(1) }

        LaunchedEffect(Unit) {
            Log.d("PoseEstimationScreen", "LaunchedEffect for Landmarker setup.")
            val landmarker = setupPoseLandmarker(
                context = context,
                runningMode = RunningMode.LIVE_STREAM,
                executor = executor,
                resultListener = { result, width, height ->
                    poseResults = result
                    inputImageWidth = width
                    inputImageHeight = height
                },
                errorListener = { error ->
                    Log.e("PoseEstimationScreen", "MediaPipe Landmarker Error: ${error?.message}")
                }
            )
            onLandmarkerInitialized(landmarker)
            Log.d("PoseEstimationScreen", "Landmarker initialization complete.")
        }

        val previewView = remember { PreviewView(context) }
        val cameraSelector = remember(lensFacing) {
            CameraSelector.Builder().requireLensFacing(lensFacing).build()
        }

        LaunchedEffect(lensFacing, poseLandmarker, hasCameraPermission) {
            val landmarkerInstance = poseLandmarker

            if (landmarkerInstance == null || !hasCameraPermission) {
                Log.d(
                    "PoseEstimationScreen",
                    "LaunchedEffect for CameraX binding: Waiting for Landmarker or Permission."
                )
                return@LaunchedEffect
            }
            Log.d(
                "PoseEstimationScreen",
                "LaunchedEffect for CameraX binding: Landmarker ready and Camera Permission granted, binding CameraX."
            )

            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also {
                    it.setAnalyzer(executor,
                        PoseAnalyzer(
                            poseLandmarker = landmarkerInstance,
                            onImageProxyClosed = { imageProxy -> imageProxy.close() }
                        )
                    )
                }

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageAnalyzer
                )
                Log.d("PoseEstimationScreen", "CameraX bound successfully.")
            } catch (exc: Exception) {
                Log.e("PoseEstimationScreen", "CameraX binding failed", exc)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Log.d("PoseEstimationScreen", "Calling PoseOverlay Composable.")

            PoseOverlay(
                poseResult = poseResults,
                imageWidth = inputImageWidth,
                imageHeight = inputImageHeight,
                isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun setupPoseLandmarker(
        context: Context,
        runningMode: RunningMode,
        executor: ExecutorService,
        modelName: String = "pose_landmarker_full.task",
        numPoses: Int = 1,
        minPoseDetectionConfidence: Float = 0.5f,
        minPosePresenceConfidence: Float = 0.5f,
        minTrackingConfidence: Float = 0.5f,
        resultListener: (result: PoseLandmarkerResult, imageWidth: Int, imageHeight: Int) -> Unit,
        errorListener: (error: Exception?) -> Unit
    ): PoseLandmarker? {
        val modelPath = modelName
        Log.d("setupPoseLandmarker", "Attempting to load model: $modelPath")

        try {
            val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath(modelPath)

            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setRunningMode(runningMode)
                .setNumPoses(numPoses)
                .setMinPoseDetectionConfidence(minPoseDetectionConfidence)
                .setMinPosePresenceConfidence(minPosePresenceConfidence)
                .setMinTrackingConfidence(minTrackingConfidence)
                .setResultListener { result, inputImage ->
                    resultListener(result, inputImage.width, inputImage.height)
                }
                .setErrorListener { error ->
                    errorListener(error)
                }

            val options = optionsBuilder.build()
            val landmarker = PoseLandmarker.createFromOptions(context, options)
            Log.d("setupPoseLandmarker", "PoseLandmarker created successfully.")
            return landmarker
        } catch (e: Exception) {
            Log.e("setupPoseLandmarker", "Failed to create PoseLandmarker", e)
            errorListener(e)
            return null
        }
    }

    private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(this).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(this))
            }
        }
}
