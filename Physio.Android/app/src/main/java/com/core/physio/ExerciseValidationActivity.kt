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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.physio.library.DynamicExerciseValidator
import com.core.physio.library.ExerciseData
import com.google.mediapipe.tasks.core.Delegate

class ExerciseValidationActivity : ComponentActivity() {

    private var hasCameraPermission by mutableStateOf(false)
    private lateinit var cameraExecutor: ExecutorService
    private var poseResults by mutableStateOf<PoseLandmarkerResult?>(null)

    private var exerciseValidator: DynamicExerciseValidator? = null
    private var exerciseData by mutableStateOf(ExerciseData())
    private var targetReps: Int = 10

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        val exerciseRulesJson = intent.getStringExtra("exerciseRulesJson")
        targetReps = intent.getIntExtra("targetReps", 10)

        if (exerciseRulesJson != null) {
            try {
                exerciseValidator = DynamicExerciseValidator.fromJson(exerciseRulesJson, targetReps)
                exerciseValidator?.reset()
                Log.d("ExerciseValidation", "Dynamic Validator started successfully")
                Log.d("ExerciseValidation", "Exercise: ${exerciseValidator?.getExerciseInfo()?.exercise_name}")
                Log.d("ExerciseValidation", "Repetitions: $targetReps")
            } catch (e: Exception) {
                Log.e("ExerciseValidation", "Error initializing validator: ${e.message}")
                finish()
                return
            }
        } else {
            Log.e("ExerciseValidation", "Don't have exercise rules JSON")
            finish()
            return
        }

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

            ExerciseUI(
                exerciseData = exerciseData,
                onReset = { exerciseValidator?.reset() },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun createPoseLandmarker(): PoseLandmarker? {
        return try {
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath("pose_landmarker_lite.task")
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

                    if (result.landmarks().isNotEmpty()) {
                        exerciseValidator?.let { validator ->
                            exerciseData = validator.validatePose(result.landmarks()[0])

                            // Verificar si la sesión se completó
                            if (exerciseData.isSessionComplete) {
                                Log.d("ExerciseValidation", "¡Sesión completada!")
                                // Opcional: Auto-finish después de un delay
                                // Handler(Looper.getMainLooper()).postDelayed({ finishSession() }, 3000)
                            }
                        }
                    }
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

    private fun setupCamera(
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

    private fun finishSession() {
        // TODO: Navegar a pantalla de resultados con métricas
        // val sessionMetrics = exerciseValidator?.getSessionMetrics()
        // val intent = Intent(this, SessionResultsActivity::class.java)
        // intent.putExtra("sessionMetrics", sessionMetrics)
        // startActivity(intent)

        Log.d("ExerciseValidation", "Finalizando sesión...")
        finish()
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
                    color = androidx.compose.ui.graphics.Color.Black,
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
                        color = androidx.compose.ui.graphics.Color.White,
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

@Composable
fun ExerciseUI(
    exerciseData: ExerciseData,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CounterCard(
                value = "${exerciseData.repCount}/20",
                backgroundColor = Color.Black.copy(alpha = 0.7f),
                textColor = Color.White
            )

            val minutes = (exerciseData.timeElapsed / 60000).toInt()
            val seconds = ((exerciseData.timeElapsed % 60000) / 1000).toInt()
            CounterCard(
                value = "%02d:%02d".format(minutes, seconds),
                backgroundColor = Color.White.copy(alpha = 0.9f),
                textColor = Color.Black
            )
        }

        FeedbackCard(
            message = exerciseData.currentMessage,
            isError = exerciseData.errorCount > 0,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        )

        FloatingActionButton(
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("R")
        }
    }
}

@Composable
fun CounterCard(
    value: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FeedbackCard(
    message: String,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isError) {
        Color.Red.copy(alpha = 0.9f)
    } else {
        Color.Green.copy(alpha = 0.9f)
    }

    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "",
            fontSize = 24.sp
        )
        Text(
            text = message,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}