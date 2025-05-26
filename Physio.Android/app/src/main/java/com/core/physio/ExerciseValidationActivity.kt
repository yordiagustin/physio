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
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                isFrontCamera = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun createPoseLandmarker(): PoseLandmarker? {
        return try {
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("pose_landmarker_full.task")
//                        .setDelegate(BaseOptions.Delegate.GPU)
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
            Log.d("SimpleAnalyzer", "Skipping frame $frameCount - still processing")
            imageProxy.close()
            return
        }

        isProcessing = true
        val startTime = System.currentTimeMillis()

        try {

            val bitmap = imageProxy.toBitmap()

            val finalBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false).also { bitmap.recycle() }
            } else {
                bitmap
            }

            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(finalBitmap).build()
            val timestampMs = System.currentTimeMillis()

            poseLandmarker.detectAsync(mpImage, timestampMs)

            finalBitmap.recycle()

            val processingTime = System.currentTimeMillis() - startTime
            Log.d("SimpleAnalyzer", "Frame $frameCount processed in ${processingTime}ms")

        } catch (e: Exception) {
            Log.e("SimpleAnalyzer", "Error: ${e.message}")
        } finally {
            isProcessing = false
            imageProxy.close()
        }
    }
}

@Composable
fun SimplePoseOverlay(
    poseResult: PoseLandmarkerResult?,
    imageWidth: Int = 640,
    imageHeight: Int = 480,
    isFrontCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (poseResult == null || poseResult.landmarks().isEmpty()) {
            return@Canvas
        }

        Log.d("SimplePoseOverlay", "🎯 Canvas: ${size.width}x${size.height}, Image: ${imageWidth}x${imageHeight}")

        poseResult.landmarks().forEachIndexed { poseIndex, landmarks ->

            if (PoseLandmarker.POSE_LANDMARKS.isNotEmpty()) {
                PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                    val startIndex = connection.start()
                    val endIndex = connection.end()

                    if (startIndex < landmarks.size && endIndex < landmarks.size) {
                        val startLandmark = landmarks[startIndex]
                        val endLandmark = landmarks[endIndex]

                        //Rotate and scale the coordinates
                        val startX = (1.0f - startLandmark.y()) * size.width
                        val startY = startLandmark.x() * size.height
                        val endX = (1.0f - endLandmark.y()) * size.width
                        val endY = endLandmark.x() * size.height

                        val finalStartX = if (isFrontCamera) size.width - startX else startX
                        val finalEndX = if (isFrontCamera) size.width - endX else endX

                        drawLine(
                            color = androidx.compose.ui.graphics.Color.Green,
                            start = androidx.compose.ui.geometry.Offset(finalStartX, startY),
                            end = androidx.compose.ui.geometry.Offset(finalEndX, endY),
                            strokeWidth = 6f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        if (startIndex in listOf(0, 11, 12, 15, 16)) {
                            Log.d("SimplePoseOverlay", "Point $startIndex: MP(${startLandmark.x()}, ${startLandmark.y()}) -> Rotated($finalStartX, $startY)")
                        }
                    }
                }
            } else {
                Log.w("SimplePoseOverlay", "POSE_LANDMARKS is empty!")
            }
        }
    }
}
