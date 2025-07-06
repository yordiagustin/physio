package com.core.physio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.core.physio.data.model.SessionResultsResponse
import com.core.physio.data.repository.ExerciseRepository
import com.core.physio.ui.theme.PhysioTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExerciseResultsActivity : ComponentActivity(){

    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        sessionId = intent.getStringExtra("sessionId") ?: "ebe7232b-526c-4b94-aa0e-e4421a16cb5e"

        setContent {
            PhysioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExerciseResultsScreen(
                        sessionId = sessionId ?: "",
                        onBackToHome = { goToHomeActivity() }
                    )
                }
            }
        }
    }

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun ExerciseResultsScreen(
    sessionId: String = "",
    onBackToHome: () -> Unit = {}
) {
    var sessionResults by remember { mutableStateOf<SessionResultsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionId) {
        if (sessionId.isNotEmpty()) {
            try {
                 val response = ExerciseRepository().getSessionResults(sessionId)
                 if (response.isSuccessful) {
                     sessionResults = response.body()
                 } else {
                     errorMessage = "Error loading results: ${response.code()}"
                 }
            } catch (e: Exception) {
                errorMessage = "Connection Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Lottie Animation for Checkmark
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.checkmark_animation))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(250.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title
            Text(
                text = if (isLoading) "Cargando resultados..." else "¡Ejercicio completado!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Subtitle message
            Text(
                text = when {
                    isLoading -> "Obteniendo indicadores de la sesión..."
                    errorMessage != null -> errorMessage!!
                    else -> "Felicitaciones ${sessionResults?.patientName?.split(" ")?.first() ?: ""} por haber completado tu sesión. A continuación te presento tus resultados:"
                },
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = if (errorMessage != null) Color.Red else Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Results Section
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PurpleColor)
                }
            } else if (sessionResults != null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

//                    Text(
//                        text = "${sessionResults!!.exerciseName} | ${formatDate(sessionResults!!.sessionDate)}",
//                        textAlign = TextAlign.Center,
//                        fontSize = 14.sp,
//                        color = Color.Gray
//                    )

                    ResultRow(
                        label = "Tiempo Efectivo",
                        value = "${formatDuration(sessionResults!!.totalDurationMinutes)}"
                    )

                    ResultRow(
                        label = "Repeticiones",
                        value = "${sessionResults!!.repetitionsCompleted}"
                    )

                    ResultRow(
                        label = "Nro. De Errores",
                        value = "${sessionResults!!.totalErrors}"
                    )

                    ResultRow(
                        label = "Rango de Movimiento",
                        value = "${sessionResults!!.avgRangeOfMotion}°"
                    )

//                    ResultRow(
//                        label = "Score General",
//                        value = "${sessionResults!!.sessionScore}%",
//                        valueColor = getScoreColor(sessionResults!!.sessionScore)
//                    )
                }
            }
        }

        // "Back to Home" Button
        Button(
            onClick = onBackToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleColor
            )
        ) {
            Text(text = "Volver a Inicio", fontSize = 16.sp)
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontWeight = FontWeight.Medium)
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = valueColor
            )
        }
    }
}

fun formatDuration(minutes: Double): String {
    val totalSeconds = (minutes * 60).toInt()
    val durationFormatted = when {
        totalSeconds < 60 -> "$totalSeconds segundos"
        totalSeconds < 120 -> "1 min ${totalSeconds - 60} seg"
        else -> "${totalSeconds / 60} min ${totalSeconds % 60} seg"
    }
    return durationFormatted
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

fun getScoreColor(score: Double): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50) // Green
        score >= 60 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseResultsScreenPreview() {
    PhysioTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ExerciseResultsScreen(
                sessionId = "test-session-id"
            )
        }
    }
}
