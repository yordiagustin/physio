import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker

class PoseAnalyzer(
    private val poseLandmarker: PoseLandmarker,
    private val onImageProxyClosed: (imageProxy: ImageProxy) -> Unit
): ImageAnalysis.Analyzer {

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val bitmap = imageProxy.toBitmap(rotationDegrees)

        if (bitmap == null) {
            Log.e("PoseAnalyzer", "Failed to convert ImageProxy to Bitmap")
            onImageProxyClosed(imageProxy)
            return
        }


        val mpImage = BitmapImageBuilder(bitmap).build()
        val frameTimestamp = System.nanoTime() / 1_000_000

        try {
            poseLandmarker.detectAsync(mpImage, frameTimestamp)

        } catch (e: Exception) {
            Log.e("PoseAnalyzer", "MediaPipe detectAsync error: ${e.message}")
        } finally {
            onImageProxyClosed(imageProxy)
            bitmap.recycle()
        }
    }

    private fun ImageProxy.toBitmap(rotationDegrees: Int): Bitmap? {
        val format = format
        if (format != android.graphics.ImageFormat.YUV_420_888) {
            Log.e("ImageProxy.toBitmap", "Unsupported format: $format. Only YUV_420_888 is supported.")
            return null
        }

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

        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, this.width, this.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return null

        if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                .also { if (it != bitmap) bitmap.recycle() }
        }
        return bitmap
    }
}
