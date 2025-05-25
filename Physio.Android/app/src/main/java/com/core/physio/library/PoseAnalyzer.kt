import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class PoseAnalyzer(
    private val poseLandmarker: PoseLandmarker,
    private val onImageProxyClosed: (imageProxy: ImageProxy) -> Unit
): ImageAnalysis.Analyzer {

    // Flags para controlar el procesamiento
    private val isProcessing = AtomicBoolean(false)
    private var frameCount = 0L
    private var lastProcessedTime = 0L

    // Cache para matrices de rotación (evitar recrear)
    private val rotationMatrixCache = mutableMapOf<Int, Matrix>()

    // Pool de ByteArrayOutputStream para reutilizar
    private val outputStreamPool = mutableListOf<ByteArrayOutputStream>()
    private val maxPoolSize = 3

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        frameCount++
        val currentTime = System.currentTimeMillis()

        if(isProcessing.get()){
            Log.d("PoseAnalyzer", "Skipping frame: $frameCount")
            onImageProxyClosed(imageProxy)
            return
        }

        if (currentTime - lastProcessedTime < 50) {
            onImageProxyClosed(imageProxy)
            return
        }

        lastProcessedTime = currentTime
        isProcessing.set(true)

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        try {
            val bitmap = imageProxy.toBitmap(rotationDegrees)

            if (bitmap == null) {
                Log.e("OptimizedPoseAnalyzer", "Failed to convert ImageProxy to Bitmap")
                return
            }

            val finalBitmap = if (bitmap.width > 1080 || bitmap.height > 1080) {
                val scaleFactor = minOf(1080f / bitmap.width, 1080f / bitmap.height)
                val newWidth = (bitmap.width * scaleFactor).toInt()
                val newHeight = (bitmap.height * scaleFactor).toInt()

                Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false).also {
                    if (it != bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            val mpImage = BitmapImageBuilder(finalBitmap).build()
            val frameTimestamp = System.nanoTime() / 1_000_000

            poseLandmarker.detectAsync(mpImage, frameTimestamp)

            finalBitmap.recycle()

            Log.d("OptimizedPoseAnalyzer", "Frame $frameCount processed in ${System.currentTimeMillis() - currentTime}ms")
        } catch (e: Exception) {
            Log.e("OptimizedPoseAnalyzer", "MediaPipe detectAsync error: ${e.message}")
        } finally {
            isProcessing.set(false)
            onImageProxyClosed(imageProxy)
        }
    }

    private fun ImageProxy.toBitmap(rotationDegrees: Int): Bitmap? {
        if (format != ImageFormat.YUV_420_888) return null

        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()

        yuvImage.compressToJpeg(Rect(0, 0, width, height), 50, out)

        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // Pool management para ByteArrayOutputStream
    private fun getOutputStream(): ByteArrayOutputStream {
        return if (outputStreamPool.isNotEmpty()) {
            outputStreamPool.removeAt(outputStreamPool.size - 1).apply { reset() }
        } else {
            ByteArrayOutputStream()
        }
    }

    private fun returnOutputStream(stream: ByteArrayOutputStream) {
        if (outputStreamPool.size < maxPoolSize) {
            outputStreamPool.add(stream)
        }
    }

    private fun getRotationMatrix(degrees: Int): Matrix {
        return rotationMatrixCache.getOrPut(degrees) {
            Matrix().apply { postRotate(degrees.toFloat()) }
        }
    }

    fun cleanUp(){
        outputStreamPool.clear()
        rotationMatrixCache.clear()
        Log.d("OptimizedPoseAnalyzer", "Resources cleaned up")
    }
}
