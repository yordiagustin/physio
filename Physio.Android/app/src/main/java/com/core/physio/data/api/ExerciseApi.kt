package com.core.physio.data.api

import com.core.physio.data.model.Exercise
import com.core.physio.data.model.SessionApiResponse
import com.core.physio.data.model.SessionResultsRequest
import com.core.physio.data.model.SessionResultsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ExerciseApi {
    @GET("api/exercises")
    suspend fun getExercises(@Query("patientId") patientId: String): List<Exercise>

    @POST("api/session")
    suspend fun saveSessionResults(@Body request: SessionResultsRequest): Response<SessionApiResponse>

    @GET("api/session/results")
    suspend fun getSessionResults(@Query("sessionId") sessionId: String): Response<SessionResultsResponse>

}

interface ExerciseRulesApi {
    @GET("api/exercises/rules")
    suspend fun getExerciseRules(@Query("exerciseId") exerciseId: String): Response<okhttp3.ResponseBody>
}
