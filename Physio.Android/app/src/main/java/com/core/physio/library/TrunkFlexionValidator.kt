package com.core.physio.library

data class ExerciseData(
    val repCount: Int = 0,
    val errorCount: Int = 0,
    val currentPhase: ExercisePhase = ExercisePhase.STARTING,
    val currentMessage: String = "Ponte en posición inicial",
    val timeElapsed: Long = 0L,
    val isActive: Boolean = false
)

data class Point(val x: Float, val y: Float)

enum class ExercisePhase {
    STARTING,
    DESCENDING,
    BOTTOM_POSITION,
    ASCENDING,
    COMPLETED_REP
}

enum class ExerciseError {
    KNEE_TOO_HIGH,
    BACK_NOT_STRAIGHT,
    INCOMPLETE_RANGE,
    TOO_FAST,
    IMPROPER_FORM
}

class TrunkFlexionValidator {
    private var exerciseData = ExerciseData()
    private var startTime = 0L
    private val angleHistory = mutableListOf<Float>()
    private val phaseTimestamps = mutableMapOf<ExercisePhase, Long>()
    private var lastValidation = 0L


    companion object {
        const val STARTING_ANGLE_MIN = 160f
        const val BOTTOM_ANGLE_MAX = 90f
        const val MIN_RANGE_DEGREES = 60f
        const val MAX_PHASE_TIME_MS = 3000L
        const val MIN_PHASE_TIME_MS = 500L
        const val VALIDATION_INTERVAL_MS = 100L
    }

    fun validatePose(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): ExerciseData {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastValidation < VALIDATION_INTERVAL_MS) {
            return exerciseData.copy(timeElapsed = currentTime - startTime)
        }
        lastValidation = currentTime

        try {

            val trunkAngle = calculateTrunkFlexionAngle(landmarks)
            val kneeAngle = calculateKneeAngle(landmarks)


            angleHistory.add(trunkAngle)
            if (angleHistory.size > 10) angleHistory.removeAt(0)


            val newPhase = determineExercisePhase(trunkAngle, exerciseData.currentPhase, currentTime)
            val errors = detectErrors(landmarks, trunkAngle, kneeAngle, newPhase, currentTime)
            val message = generateFeedbackMessage(newPhase, errors, trunkAngle)


            var newRepCount = exerciseData.repCount
            if (newPhase == ExercisePhase.COMPLETED_REP && exerciseData.currentPhase != ExercisePhase.COMPLETED_REP) {
                newRepCount++
            }

            var newErrorCount = exerciseData.errorCount
            if (errors.isNotEmpty()) {
                newErrorCount++
            }

            exerciseData = ExerciseData(
                repCount = newRepCount,
                errorCount = newErrorCount,
                currentPhase = newPhase,
                currentMessage = message,
                timeElapsed = currentTime - startTime,
                isActive = true
            )

        } catch (e: Exception) {
            android.util.Log.e("TrunkFlexionValidator", "Error validating pose: ${e.message}")
        }

        return exerciseData
    }

    private fun calculateTrunkFlexionAngle(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val shoulder = landmarks[11]
        val hip = landmarks[23]
        val knee = landmarks[25]

        return calculateAngleBetweenPoints(
            Point(shoulder.x(), shoulder.y()),
            Point(hip.x(), hip.y()),
            Point(knee.x(), knee.y())
        )
    }

    private fun calculateKneeAngle(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Float {
        val hip = landmarks[23]
        val knee = landmarks[25]
        val ankle = landmarks[27]

        return calculateAngleBetweenPoints(
            Point(hip.x(), hip.y()),
            Point(knee.x(), knee.y()),
            Point(ankle.x(), ankle.y())
        )
    }

    private fun calculateAngleBetweenPoints(p1: Point, center: Point, p3: Point): Float {
        val angle1 = kotlin.math.atan2(p1.y - center.y, p1.x - center.x)
        val angle2 = kotlin.math.atan2(p3.y - center.y, p3.x - center.x)
        var angleDiff = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()

        if (angleDiff < 0) angleDiff += 360f
        if (angleDiff > 180f) angleDiff = 360f - angleDiff

        return kotlin.math.abs(angleDiff)
    }

    private fun determineExercisePhase(trunkAngle: Float, currentPhase: ExercisePhase, currentTime: Long): ExercisePhase {
        return when (currentPhase) {
            ExercisePhase.STARTING -> {
                if (trunkAngle < STARTING_ANGLE_MIN) {
                    phaseTimestamps[ExercisePhase.DESCENDING] = currentTime
                    ExercisePhase.DESCENDING
                } else ExercisePhase.STARTING
            }

            ExercisePhase.DESCENDING -> {
                if (trunkAngle <= BOTTOM_ANGLE_MAX) {
                    phaseTimestamps[ExercisePhase.BOTTOM_POSITION] = currentTime
                    ExercisePhase.BOTTOM_POSITION
                } else ExercisePhase.DESCENDING
            }

            ExercisePhase.BOTTOM_POSITION -> {
                if (trunkAngle > BOTTOM_ANGLE_MAX + 10f) { // Hysteresis de 10°
                    phaseTimestamps[ExercisePhase.ASCENDING] = currentTime
                    ExercisePhase.ASCENDING
                } else ExercisePhase.BOTTOM_POSITION
            }

            ExercisePhase.ASCENDING -> {
                if (trunkAngle >= STARTING_ANGLE_MIN - 10f) { // Hysteresis de 10°
                    phaseTimestamps[ExercisePhase.COMPLETED_REP] = currentTime
                    ExercisePhase.COMPLETED_REP
                } else ExercisePhase.ASCENDING
            }

            ExercisePhase.COMPLETED_REP -> {
                ExercisePhase.STARTING
            }
        }
    }

    private fun detectErrors(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        trunkAngle: Float,
        kneeAngle: Float,
        phase: ExercisePhase,
        currentTime: Long
    ): List<ExerciseError> {
        val errors = mutableListOf<ExerciseError>()

        if (phase == ExercisePhase.BOTTOM_POSITION && kneeAngle < 70f) {
            errors.add(ExerciseError.KNEE_TOO_HIGH)
        }

        if (phase == ExercisePhase.BOTTOM_POSITION && trunkAngle > BOTTOM_ANGLE_MAX + 20f) {
            errors.add(ExerciseError.INCOMPLETE_RANGE)
        }

        val phaseStartTime = phaseTimestamps[phase] ?: currentTime
        val phaseTime = currentTime - phaseStartTime
        if (phaseTime < MIN_PHASE_TIME_MS && phase != ExercisePhase.STARTING) {
            errors.add(ExerciseError.TOO_FAST)
        }

        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val shoulderDifference = kotlin.math.abs(leftShoulder.y() - rightShoulder.y())
        if (shoulderDifference > 0.05f) {
            errors.add(ExerciseError.BACK_NOT_STRAIGHT)
        }

        return errors
    }

    private fun generateFeedbackMessage(phase: ExercisePhase, errors: List<ExerciseError>, trunkAngle: Float): String {

        if (errors.isNotEmpty()) {
            return when (errors.first()) {
                ExerciseError.KNEE_TOO_HIGH -> "Baja más la rodilla hacia el suelo"
                ExerciseError.BACK_NOT_STRAIGHT -> "Mantén la espalda recta"
                ExerciseError.INCOMPLETE_RANGE -> "Flexiona más el tronco"
                ExerciseError.TOO_FAST -> "Ve más despacio, controla el movimiento"
                ExerciseError.IMPROPER_FORM -> "Corrige la postura"
            }
        }

        return when (phase) {
            ExercisePhase.STARTING -> "Posición inicial correcta"
            ExercisePhase.DESCENDING -> "Flexiona el tronco hacia la rodilla"
            ExercisePhase.BOTTOM_POSITION -> "¡Perfecto! Ahora regresa despacio"
            ExercisePhase.ASCENDING -> "Levanta el tronco lentamente"
            ExercisePhase.COMPLETED_REP -> "¡Repetición completada!"
        }
    }

    fun reset() {
        exerciseData = ExerciseData()
        startTime = 0L
        angleHistory.clear()
        phaseTimestamps.clear()
    }
}