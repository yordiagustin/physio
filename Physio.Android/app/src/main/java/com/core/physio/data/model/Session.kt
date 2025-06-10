package com.core.physio.data.model

import com.core.physio.library.SessionMetrics
import kotlinx.serialization.Serializable

@Serializable
data class SessionResultsRequest(
    val sessionUuid: String,
    val patientId: String,
    val exerciseId: Int,
    val sessionStart: Long,
    val sessionEnd: Long?,
    val totalReps: Int,
    val successfulReps: Int,
    val totalErrors: Int,
    val sessionScore: Double,
    val notes: String?,
    val repetitions: List<RepetitionRequest>
)

@Serializable
data class RepetitionRequest(
    val repNumber: Int,
    val startTime: Long,
    val endTime: Long?,
    val effectiveTimeMs: Int,
    val rangeOfMotionDegrees: Double,
    val errorCount: Int,
    val maxAngleReached: Double,
    val minAngleReached: Double,
    val qualityScore: Double,
    val errors: List<RepetitionErrorRequest>
)

@Serializable
data class RepetitionErrorRequest(
    val errorCode: String,
    val detectedAt: Long
)

@Serializable
data class SessionApiResponse(
    val message: String,
    val sessionId: Int,
    val sessionUuid: String
)

@Serializable
data class SessionResultsResponse(
    val sessionUuid: String,
    val patientName: String,
    val exerciseName: String,
    val sessionDate: String,
    val sessionStart: String,
    val sessionEnd: String?,
    val totalDurationMinutes: Double,
    val repetitionsCompleted: Int,
    val totalErrors: Int,
    val avgRangeOfMotion: Double,
    val sessionScore: Double
)

//Extension function to convert SessionMetrics to SessionResultsRequest
fun SessionMetrics.toApiRequest(patientId: String, notes: String? = null): SessionResultsRequest {
    return SessionResultsRequest(
        sessionUuid = sessionUuid,
        patientId = patientId,
        exerciseId = exerciseId,
        sessionStart = sessionStart,
        sessionEnd = sessionEnd,
        totalReps = totalReps,
        successfulReps = successfulReps,
        totalErrors = totalErrors,
        sessionScore = sessionScore.toDouble(),
        notes = notes,
        repetitions = repetitions.map { rep ->
            RepetitionRequest(
                repNumber = rep.repNumber,
                startTime = rep.startTime,
                endTime = rep.endTime,
                effectiveTimeMs = rep.effectiveTimeMs,
                rangeOfMotionDegrees = rep.rangeOfMotionDegrees.toDouble(),
                errorCount = rep.errorCount,
                maxAngleReached = rep.maxAngleReached.toDouble(),
                minAngleReached = rep.minAngleReached.toDouble(),
                qualityScore = rep.qualityScore.toDouble(),
                errors = rep.errors.map { error ->
                    RepetitionErrorRequest(
                        errorCode = error.errorCode,
                        detectedAt = error.detectedAt
                    )
                }
            )
        }
    )
}