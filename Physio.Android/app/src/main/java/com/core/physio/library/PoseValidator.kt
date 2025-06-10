package com.core.physio.library

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Data classes para deserializar el JSON del API
@Serializable
data class ExerciseConfig(
    val exercise_id: Int,
    val exercise_name: String,
    val description: String,
    val target_condition: String,
    val difficulty_level: Int,
    val estimated_duration_minutes: Int,
    val phases: List<PhaseConfig>,
    val parameters: Map<String, ParameterConfig>,
    val error_types: List<ErrorTypeConfig>,
    val validation_rules: List<ValidationRuleConfig>,
    val landmark_mappings: Map<String, LandmarkMappingConfig>
)

@Serializable
data class PhaseConfig(
    val phase_name: String,
    val phase_order: Int,
    val instruction_message: String,
    val success_message: String?,
    val transitions: List<TransitionConfig>
)

@Serializable
data class TransitionConfig(
    val parameter_name: String,
    val operator: String,
    val value: Double,
    val value2: Double? = null,
    val hysteresis: Double
)

@Serializable
data class ParameterConfig(
    val name: String,
    val type: String,
    val phase_specific: String? = null,
    val default_value: Double,
    val min_value: Double,
    val max_value: Double,
    val unit: String,
    val description: String? = null
)

@Serializable
data class ErrorTypeConfig(
    val error_code: String,
    val error_name: String,
    val error_category: String,
    val severity: Int,
    val feedback_message: String,
    val correction_hint: String? = null
)

@Serializable
data class ValidationRuleConfig(
    val rule_type: String,
    val applicable_phases: List<String>,
    val parameters: Map<String, String>,
    val error_code: String,
    val priority: Int,
    val is_active: Boolean
)

@Serializable
data class LandmarkMappingConfig(
    val mapping_type: String,
    val joint_name: String,
    val description: String? = null,
    val indices: List<Int>
)

// Datos actuales del ejercicio para el UI
data class ExerciseData(
    val repCount: Int = 0,
    val errorCount: Int = 0,
    val currentPhase: String = "", // Dinámico basado en la primera fase del ejercicio
    val currentMessage: String = "",
    val timeElapsed: Long = 0L,
    val isActive: Boolean = false,
    val primaryAngle: Float = 0f, // Ángulo del landmark mapping primario
    val allAngles: Map<String, Float> = emptyMap(), // Todos los ángulos calculados
    val detectedErrors: List<String> = emptyList(),
    // Campos para gestión de sesión
    val targetReps: Int = 10, // Meta de repeticiones
    val isSessionComplete: Boolean = false,
    val sessionProgress: Float = 0f // Porcentaje de progreso (0.0 - 1.0)
)

data class Point(val x: Float, val y: Float)

class DynamicExerciseValidator(
    private val exerciseConfig: ExerciseConfig,
    private val targetReps: Int = 10
) {
    private var exerciseData = ExerciseData(targetReps = targetReps)
    private var sessionStartTime = 0L
    private val angleHistory = mutableListOf<Float>()
    private val phaseTimestamps = mutableMapOf<String, Long>()
    private var lastValidation = 0L

    // Cache de valores calculados para optimización
    private val phaseByOrder = exerciseConfig.phases.associateBy { it.phase_order }
    private val phaseByName = exerciseConfig.phases.associateBy { it.phase_name }
    private val errorMessages = exerciseConfig.error_types.associateBy { it.error_code }
    private val validationRulesByPhase = exerciseConfig.validation_rules
        .filter { it.is_active }
        .groupBy { it.applicable_phases }
        .flatMap { (phases, rules) -> phases.flatMap { phase -> rules.map { phase to it } } }
        .groupBy { it.first }
        .mapValues { it.value.map { pair -> pair.second } }

    // Obtener el landmark mapping primario (el primero que encuentre de tipo primary_joint)
    private val primaryLandmarkMapping = exerciseConfig.landmark_mappings.values
        .firstOrNull { it.mapping_type == "primary_joint" }

    // Determinar la fase inicial dinámicamente
    private val initialPhase = exerciseConfig.phases.minByOrNull { it.phase_order }?.phase_name ?: "UNKNOWN"

    companion object {
        const val VALIDATION_INTERVAL_MS = 100L

        // Factory method para crear desde JSON
        fun fromJson(jsonString: String, targetReps: Int = 10): DynamicExerciseValidator {
            val config = Json.decodeFromString<ExerciseConfig>(jsonString)
            return DynamicExerciseValidator(config, targetReps)
        }
    }

    fun validatePose(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): ExerciseData {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastValidation < VALIDATION_INTERVAL_MS) {
            return exerciseData.copy(timeElapsed = currentTime - sessionStartTime)
        }
        lastValidation = currentTime

        try {
            // Calcular todos los ángulos basados en landmark_mappings
            val angles = calculateAngles(landmarks)

            // Obtener el ángulo primario dinámicamente
            val primaryAngle = getPrimaryAngle(angles)

            // Agregar ángulo primario a historial
            angleHistory.add(primaryAngle)
            if (angleHistory.size > 10) angleHistory.removeAt(0)

            // Determinar nueva fase
            val newPhase = determineExercisePhase(angles, exerciseData.currentPhase, currentTime)

            // Detectar errores usando las reglas de validación
            val errors = detectErrors(landmarks, angles, newPhase, currentTime)

            // Generar mensaje de feedback
            val message = generateFeedbackMessage(newPhase, errors)

            // Actualizar contadores
            var newRepCount = exerciseData.repCount
            val wasRepCompleted = isCompletedRep(newPhase, exerciseData.currentPhase)

            if (wasRepCompleted) {
                newRepCount++
                android.util.Log.d("DynamicValidator", "¡Repetición completada! Total: $newRepCount/$targetReps")
            }

            var newErrorCount = exerciseData.errorCount
            if (errors.isNotEmpty()) {
                newErrorCount++
            }

            // Calcular progreso y verificar si la sesión está completa
            val sessionProgress = newRepCount.toFloat() / targetReps.toFloat()
            val isSessionComplete = newRepCount >= targetReps

            val finalMessage = when {
                isSessionComplete -> "¡Sesión completada! Excelente trabajo 🎉"
                else -> message
            }

            exerciseData = ExerciseData(
                repCount = newRepCount,
                errorCount = newErrorCount,
                currentPhase = newPhase,
                currentMessage = finalMessage,
                timeElapsed = currentTime - sessionStartTime,
                isActive = !isSessionComplete,
                primaryAngle = primaryAngle,
                allAngles = angles,
                detectedErrors = errors,
                targetReps = targetReps,
                isSessionComplete = isSessionComplete,
                sessionProgress = sessionProgress
            )

        } catch (e: Exception) {
            android.util.Log.e("DynamicExerciseValidator", "Error validating pose: ${e.message}")
        }

        return exerciseData
    }

    private fun calculateAngles(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Map<String, Float> {
        val angles = mutableMapOf<String, Float>()

        exerciseConfig.landmark_mappings.forEach { (key, mapping) ->
            if (mapping.indices.size >= 3) {
                val p1 = landmarks[mapping.indices[0]]
                val center = landmarks[mapping.indices[1]]
                val p3 = landmarks[mapping.indices[2]]

                val angle = calculateAngleBetweenPoints(
                    Point(p1.x(), p1.y()),
                    Point(center.x(), center.y()),
                    Point(p3.x(), p3.y())
                )

                // Usar el nombre del joint directamente del mapping
                angles[mapping.joint_name] = angle
            }
        }

        return angles
    }

    private fun calculateAngleBetweenPoints(p1: Point, center: Point, p3: Point): Float {
        val angle1 = kotlin.math.atan2(p1.y - center.y, p1.x - center.x)
        val angle2 = kotlin.math.atan2(p3.y - center.y, p3.x - center.x)
        var angleDiff = Math.toDegrees((angle2 - angle1).toDouble()).toFloat()

        if (angleDiff < 0) angleDiff += 360f
        if (angleDiff > 180f) angleDiff = 360f - angleDiff

        return kotlin.math.abs(angleDiff)
    }

    private fun getPrimaryAngle(angles: Map<String, Float>): Float {
        // Obtener el ángulo del landmark mapping primario
        return primaryLandmarkMapping?.let { mapping ->
            angles[mapping.joint_name] ?: 0f
        } ?: 0f
    }

    private fun determineExercisePhase(angles: Map<String, Float>, currentPhase: String, currentTime: Long): String {
        val currentPhaseConfig = phaseByName[currentPhase] ?: return currentPhase

        // Verificar transiciones de la fase actual
        currentPhaseConfig.transitions.forEach { transition ->
            // Obtener el valor del parámetro dinámicamente
            val parameterValue = getParameterValue(transition.parameter_name, angles)

            val shouldTransition = when (transition.operator) {
                "<" -> parameterValue < transition.value
                "<=" -> parameterValue <= transition.value
                ">" -> parameterValue > transition.value
                ">=" -> parameterValue >= transition.value
                "==" -> kotlin.math.abs(parameterValue - transition.value) < transition.hysteresis
                "between" -> transition.value2?.let {
                    parameterValue >= transition.value && parameterValue <= it
                } ?: false
                else -> false
            }

            if (shouldTransition) {
                val nextPhaseOrder = currentPhaseConfig.phase_order + 1
                val nextPhase = phaseByOrder[nextPhaseOrder]
                if (nextPhase != null) {
                    phaseTimestamps[nextPhase.phase_name] = currentTime
                    android.util.Log.d("DynamicValidator", "Transición: $currentPhase → ${nextPhase.phase_name}")
                    return nextPhase.phase_name
                }
            }
        }

        return currentPhase
    }

    private fun getParameterValue(parameterName: String, angles: Map<String, Float>): Double {
        // Buscar en los ángulos calculados por el nombre exacto del parámetro
        return angles[parameterName]?.toDouble() ?: 0.0
    }

    private fun detectErrors(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        angles: Map<String, Float>,
        phase: String,
        currentTime: Long
    ): List<String> {
        val errors = mutableListOf<String>()
        val currentPhaseRules = validationRulesByPhase[phase] ?: emptyList()

        currentPhaseRules.forEach { rule ->
            val errorDetected = when (rule.rule_type) {
                "angle_check" -> checkAngleRule(rule, angles)
                "time_check" -> checkTimeRule(rule, phase, currentTime)
                "symmetry_check" -> checkSymmetryRule(rule, landmarks)
                "position_check" -> checkPositionRule(rule, landmarks, angles)
                else -> false
            }

            if (errorDetected) {
                errors.add(rule.error_code)
                android.util.Log.d("DynamicValidator", "Error detectado: ${rule.error_code} en fase $phase")
            }
        }

        return errors
    }

    private fun checkAngleRule(rule: ValidationRuleConfig, angles: Map<String, Float>): Boolean {
        val parameter = rule.parameters["parameter"] ?: return false
        val angle = angles[parameter] ?: return false

        rule.parameters["min_value"]?.toFloatOrNull()?.let { minValue ->
            if (angle < minValue) return true
        }

        rule.parameters["max_value"]?.toFloatOrNull()?.let { maxValue ->
            if (angle > maxValue) return true
        }

        return false
    }

    private fun checkTimeRule(rule: ValidationRuleConfig, phase: String, currentTime: Long): Boolean {
        val phaseStartTime = phaseTimestamps[phase] ?: currentTime
        val phaseTime = currentTime - phaseStartTime

        rule.parameters["min_time_ms"]?.toLongOrNull()?.let { minTime ->
            if (phaseTime < minTime && phaseTime > 0) return true
        }

        rule.parameters["max_time_ms"]?.toLongOrNull()?.let { maxTime ->
            if (phaseTime > maxTime) return true
        }

        return false
    }

    private fun checkSymmetryRule(rule: ValidationRuleConfig, landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>): Boolean {
        val maxAsymmetry = rule.parameters["max_asymmetry"]?.toFloatOrNull() ?: 0.05f
        val parameterName = rule.parameters["parameter"] ?: return false

        // Buscar el landmark mapping correspondiente al parámetro
        val mapping = exerciseConfig.landmark_mappings.values.find {
            it.joint_name == parameterName
        }

        mapping?.let {
            if (it.indices.size >= 2) {
                val point1 = landmarks[it.indices[0]]
                val point2 = landmarks[it.indices[1]]
                val difference = kotlin.math.abs(point1.y() - point2.y())
                return difference > maxAsymmetry
            }
        }

        return false
    }

    private fun checkPositionRule(rule: ValidationRuleConfig, landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>, angles: Map<String, Float>): Boolean {
        // Implementar verificaciones de posición específicas según sea necesario
        return false
    }

    private fun generateFeedbackMessage(phase: String, errors: List<String>): String {
        // Priorizar mensajes de error por severidad
        if (errors.isNotEmpty()) {
            val priorityError = errors.minByOrNull { error ->
                errorMessages[error]?.severity ?: Int.MAX_VALUE
            }
            return errorMessages[priorityError]?.feedback_message ?: "Corrige la postura"
        }

        // Mensaje de fase normal
        val phaseConfig = phaseByName[phase]
        return phaseConfig?.instruction_message ?: "Continúa con el ejercicio"
    }

    private fun isCompletedRep(newPhase: String, currentPhase: String): Boolean {
        return newPhase.contains("COMPLETED") && currentPhase != newPhase
    }

    fun reset() {
        exerciseData = ExerciseData(currentPhase = initialPhase, targetReps = targetReps)
        sessionStartTime = System.currentTimeMillis()
        angleHistory.clear()
        phaseTimestamps.clear()
        // Establecer timestamp para la fase inicial
        phaseTimestamps[initialPhase] = sessionStartTime
        android.util.Log.d("DynamicValidator", "Validator reseteado - Ejercicio: ${exerciseConfig.exercise_name}")
        android.util.Log.d("DynamicValidator", "Fase inicial: $initialPhase, Objetivo: $targetReps repeticiones")
    }

    fun getExerciseInfo(): ExerciseConfig = exerciseConfig

    fun getCurrentPhaseInfo(): PhaseConfig? = phaseByName[exerciseData.currentPhase]

    fun getAllPhases(): List<PhaseConfig> = exerciseConfig.phases.sortedBy { it.phase_order }

    fun getProgressSummary(): String {
        return "Repeticiones: ${exerciseData.repCount}/$targetReps (${(exerciseData.sessionProgress * 100).toInt()}%)"
    }
}