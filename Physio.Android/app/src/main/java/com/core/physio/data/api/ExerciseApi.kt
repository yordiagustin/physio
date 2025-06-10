package com.core.physio.data.api

import com.core.physio.data.model.Exercise
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExerciseApi {
    @GET("api/exercises")
    suspend fun getExercises(@Query("patientId") patientId: String): List<Exercise>
}

interface ExerciseRulesApi {
    @GET("api/exercises/rules")
    suspend fun getExerciseRules(@Query("exerciseId") exerciseId: String): Response<okhttp3.ResponseBody>
}