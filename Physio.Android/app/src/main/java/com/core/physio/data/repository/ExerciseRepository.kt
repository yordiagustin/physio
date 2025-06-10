package com.core.physio.data.repository
import com.core.physio.data.api.ExerciseApi
import com.core.physio.data.api.ExerciseRulesApi
import com.core.physio.data.model.Exercise
import com.core.physio.data.model.SessionApiResponse
import com.core.physio.data.model.SessionResultsRequest
import com.core.physio.data.model.SessionResultsResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExerciseRepository {
    private val api: ExerciseApi
    private val rulesApi: ExerciseRulesApi

    init {
        val retrofitWithGson = Retrofit.Builder()
            .baseUrl("https://physio-api-ejgvewgkfjbsfraq.brazilsouth-01.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val retrofitRaw = Retrofit.Builder()
            .baseUrl("https://physio-api-ejgvewgkfjbsfraq.brazilsouth-01.azurewebsites.net/")
            .build()

        api = retrofitWithGson.create(ExerciseApi::class.java)
        rulesApi = retrofitRaw.create(ExerciseRulesApi::class.java)
    }

    suspend fun getExercises(patientId: String): List<Exercise> {
        return api.getExercises(patientId)
    }

    suspend fun saveSessionResults(request: SessionResultsRequest): Response<SessionApiResponse> {
        return api.saveSessionResults(request)
    }

    suspend fun getSessionResults(sessionId: String): Response<SessionResultsResponse> {
        return api.getSessionResults(sessionId)
    }

    suspend fun getExerciseRules(exerciseId: String): Response<String> {
        val response = rulesApi.getExerciseRules(exerciseId)
        return if (response.isSuccessful) {
            val jsonString = response.body()?.string() ?: ""
            Response.success(jsonString)
        } else {
            Response.error(response.code(), response.errorBody() ?: okhttp3.ResponseBody.create(null, ""))
        }
    }
} 