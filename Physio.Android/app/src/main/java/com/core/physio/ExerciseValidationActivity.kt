package com.core.physio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.util.Size
import com.google.mediapipe.tasks.core.Delegate

class ExerciseValidationActivity : ComponentActivity() {

    private var hasCameraPermission by mutableStateOf(false)
    private lateinit var cameraExecutor: ExecutorService
    private var poseResults by mutableStateOf<PoseLandmarkerResult?>(null)
    private var imageWidth by mutableStateOf(640)
    private var imageHeight by mutableStateOf(480)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        checkCameraPermission()

        setContent {
            PhysioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (hasCameraPermission)
                        SimpleCameraScreen()
                    else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Needs camera permission to continue.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        LaunchedEffect(Unit) { checkCameraPermission() }
                    }
                }
            }
        }
    }

    @Composable
    private fun SimpleCameraScreen(){
        val context = LocalContext.current
        var lifecycleOwner = LocalLifecycleOwner.current

        var poseLandmarker by remember { mutableStateOf<PoseLandmarker?>(null) }

        val previewView = remember { PreviewView(context) }

        LaunchedEffect(Unit) {
            poseLandmarker = createPoseLandmarker()
        }

        LaunchedEffect(poseLandmarker) {
            val landmarker = poseLandmarker ?: return@LaunchedEffect
            setupCamera(context, lifecycleOwner, previewView, landmarker)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            SimplePoseOverlay(
                poseResult = poseResults,
                modifier = Modifier.fillMaxSize(),
                isFrontCamera = false
            )
        }
    }

    private fun createPoseLandmarker(): PoseLandmarker? {
        return try {
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("pose_landmarker_full.task")
                        .setDelegate(Delegate.GPU)
                        .build()
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumPoses(1)
                .setMinPoseDetectionConfidence(0.5f)
                .setMinPosePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, inputImage ->
                    poseResults = result
                    imageWidth = inputImage.width
                    imageHeight = inputImage.height
                }
                .setErrorListener { error ->
                    Log.e("MediaPipe", "Error: ${error?.message}")
                }
                .build()

            PoseLandmarker.createFromOptions(this, options)
        } catch (e: Exception) {
            Log.e("MediaPipe", "Setup failed: ${e.message}")
            null
        }
    }

    private suspend fun setupCamera(
        context: android.content.Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: PreviewView,
        poseLandmarker: PoseLandmarker
    ){
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()

        try {
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build()
            preview.surfaceProvider = previewView.surfaceProvider

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 360))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalyzer.setAnalyzer(cameraExecutor, SimpleAnalyzer(poseLandmarker))

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            Log.d("Camera", "Setup complete, camera bound successfully.")

        } catch (e: Exception) {
            Log.e("CameraSetup", "Error setting up camera: ${e.message}")
        }
    }

    private fun checkCameraPermission() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> hasCameraPermission = true
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

class SimpleAnalyzer(
    private val poseLandmarker: PoseLandmarker
) : ImageAnalysis.Analyzer {

    private var isProcessing = false
    private var frameCount = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
        frameCount++

        if (frameCount % 3 != 0L) {
            imageProxy.close()
            return
        }

        if (isProcessing) {
            imageProxy.close()
            return
        }

        isProcessing = true
        val startTime = System.currentTimeMillis()

        try {
            // Direct Conversion from YUV to RGB
            val bitmap = imageProxy.toBitmap()

            val matrix = android.graphics.Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(rotatedBitmap).build()
            poseLandmarker.detectAsync(mpImage, System.currentTimeMillis())

            bitmap.recycle()
            rotatedBitmap.recycle()

            val processingTime = System.currentTimeMillis() - startTime
            Log.d("SimpleAnalyzer", "Frame $frameCount processed in ${processingTime}ms")

        } catch (e: Exception) {
            Log.e("SimpleAnalyzer", "Error: ${e.message}")
            e.printStackTrace()
        } finally {
            isProcessing = false
            imageProxy.close()
        }
    }
}

@Composable
fun SimplePoseOverlay(
    poseResult: PoseLandmarkerResult?,
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean = false
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (poseResult == null || poseResult.landmarks().isEmpty()) {
            return@Canvas
        }

        for (landmark in poseResult.landmarks()) {

            landmark.forEachIndexed { index, normalizedLandmark ->
                val x = normalizedLandmark.x() * size.width
                val y = normalizedLandmark.y() * size.height

                val finalX = if (isFrontCamera) size.width - x else x

                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Red,
                    radius = 12f,
                    center = androidx.compose.ui.geometry.Offset(finalX, y)
                )
            }

            PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                if (connection != null && connection.start() < landmark.size && connection.end() < landmark.size) {
                    val startLandmark = landmark[connection.start()]
                    val endLandmark = landmark[connection.end()]

                    val startX = startLandmark.x() * size.width
                    val startY = startLandmark.y() * size.height
                    val endX = endLandmark.x() * size.width
                    val endY = endLandmark.y() * size.height

                    val finalStartX = if (isFrontCamera) size.width - startX else startX
                    val finalEndX = if (isFrontCamera) size.width - endX else endX

                    drawLine(
                        color = androidx.compose.ui.graphics.Color.Green,
                        start = androidx.compose.ui.geometry.Offset(finalStartX, startY),
                        end = androidx.compose.ui.geometry.Offset(finalEndX, endY),
                        strokeWidth = 6f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}