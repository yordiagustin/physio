package com.core.physio.data.model

data class Exercise(
    val exerciseId: Int,
    val exerciseName: String,
    val description: String,
    val estimatedDurationMinutes: Int,
    val difficultyLevel: Int,
    val instructions: String,
    val repetitions: Int
) 