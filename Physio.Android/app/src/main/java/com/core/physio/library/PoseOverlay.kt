import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker

@Composable
fun PoseOverlay(
    poseResult: PoseLandmarkerResult?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (poseResult == null || poseResult.landmarks().isEmpty()) {
            return@Canvas
        }

        Log.d("PoseOverlay", "Canvas drawing: ${poseResult.landmarks().size} pose lists detected.")

        val scaleFactorX = size.width / imageWidth.toFloat()
        val scaleFactorY = size.height / imageHeight.toFloat()
        Log.d("PoseOverlay", "Scale Factors: X=$scaleFactorX, Y=$scaleFactorY. Canvas Size: ${size.width}x${size.height}")

        poseResult.landmarks().forEachIndexed { poseIndex, landmarks ->
            Log.d("PoseOverlay", "Pose $poseIndex: ${landmarks.size} landmarks in this list.")

            if (PoseLandmarker.POSE_LANDMARKS.isEmpty()) {
                Log.w("PoseOverlay", "PoseLandmarker.POSE_LANDMARKS is empty! Cannot draw connections.")
            } else {
                Log.d("PoseOverlay", "Attempting to draw ${PoseLandmarker.POSE_LANDMARKS.size} connections.")
            }

            PoseLandmarker.POSE_LANDMARKS.forEach { connection ->
                val startIndex = connection.start()
                val endIndex = connection.end()

                if (startIndex < landmarks.size && endIndex < landmarks.size) {
                    val startLandmark = landmarks[startIndex]
                    val endLandmark = landmarks[endIndex]

                    val startX = startLandmark.x() * imageWidth * scaleFactorX
                    val startY = startLandmark.y() * imageHeight * scaleFactorY

                    val endX = endLandmark.x() * imageWidth * scaleFactorX
                    val endY = endLandmark.y() * imageHeight * scaleFactorY

                    val finalStartX = if (isFrontCamera) size.width - startX else startX
                    val finalEndX = if (isFrontCamera) size.width - endX else endX

                    drawLine(
                        color = Color.Green,
                        start = Offset(finalStartX, startY),
                        end = Offset(finalEndX, endY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    Log.d("PoseOverlay", "Skipping connection ($startIndex -> $endIndex): indices out of bounds for landmarks list size ${landmarks.size}")
                }
            }
        }
    }
}
