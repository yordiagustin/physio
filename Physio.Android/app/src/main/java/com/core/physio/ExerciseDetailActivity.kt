package com.core.physio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.core.physio.ui.theme.PhysioTheme
import kotlinx.coroutines.launch
import com.core.physio.data.repository.ExerciseRepository

class ExerciseDetailActivity : ComponentActivity() {
    private val exerciseRepository = ExerciseRepository()

    private var exerciseRulesJson: String? = null
    private var exerciseId: Int = 0
    private var targetReps: Int = 0

    private var _rulesLoaded = mutableStateOf(false)
    private var _isLoadingRules = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        exerciseId = intent.getIntExtra("exerciseId", 1)
        targetReps = intent.getIntExtra("repetitions", 10)
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val duration = intent.getStringExtra("duration") ?: ""
        val level = intent.getStringExtra("level") ?: ""
        val instructions = intent.getStringExtra("instructions") ?: ""
        val repetitions = targetReps.toString()
        val imageRes = intent.getIntExtra("imageRes", android.R.drawable.ic_menu_gallery)

        setContent {
            PhysioTheme {
                ExerciseDetailScreen(
                    title = title,
                    description = description,
                    duration = duration,
                    level = level,
                    instructions = instructions,
                    imageRes = imageRes,
                    repetitions = repetitions,
                    onStartExercise = { startExerciseActivity() },
                    isLoadingRules = _isLoadingRules.value,
                    rulesLoaded = _rulesLoaded.value
                )
            }
        }

        loadExerciseRules()
    }

    private fun loadExerciseRules() {
        _isLoadingRules.value = true
        lifecycleScope.launch {
            try {
                val response = exerciseRepository.getExerciseRules(exerciseId.toString())
                android.util.Log.d("Physiiiioo", "Response Rules: $response")
                if (response.isSuccessful) {
                    exerciseRulesJson = response.body()
                    _rulesLoaded.value = true
                    android.util.Log.d("ExerciseDetail", "Rules loaded successfully for exercise: $exerciseId")
                } else {
                    _rulesLoaded.value = false
                    android.util.Log.e("ExerciseDetail", "Error loading rules: ${response.code()}")
                }
            } catch (e: Exception) {
                _rulesLoaded.value = false
                android.util.Log.e("ExerciseDetail", "Connection Error: ${e.message}")
            } finally {
                _isLoadingRules.value = false
            }
        }
    }

    private fun startExerciseActivity() {
        if (exerciseRulesJson != null) {
            val intent = Intent(this, ExerciseValidationActivity::class.java)
            intent.putExtra("exerciseRulesJson", exerciseRulesJson)
            intent.putExtra("targetReps", targetReps)
            startActivity(intent)

            android.util.Log.d("ExerciseDetail", "Going to ExerciseValidationActivity")
        } else {
            android.util.Log.w("ExerciseDetail", "Rules not loaded yet, try again...")
        }
    }
}

@Composable
fun ExerciseDetailScreen(
    title: String,
    description: String,
    duration: String,
    level: String,
    instructions: String,
    repetitions: String,
    imageRes: Int,
    onStartExercise: () -> Unit = {},
    isLoadingRules: Boolean = false,
    rulesLoaded: Boolean = false
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAEAFE), shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.exercise_detail),
                contentDescription = null,
                modifier = Modifier
                    .height(420.dp)
                    .fillMaxWidth()
                    .align(Alignment.Center)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                description,
                modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Repeticiones", fontSize = 14.sp, color = Color.Gray)
                    Text(repetitions, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duración", fontSize = 14.sp, color = Color.Gray)
                    Text(duration, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dificultad", fontSize = 14.sp, color = Color.Gray)
                    Text("3/5", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Instrucciones", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                instructions,
                modifier = Modifier.padding(horizontal = 32.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = onStartExercise,
                enabled = rulesLoaded && !isLoadingRules,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (rulesLoaded) PurpleColor else Color.Gray
                )
            ) {
                Text(
                    if (isLoadingRules) "Preparando..." else "Iniciar Ahora",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExerciseDetailActivityPreview(){
    PhysioTheme {
        ExerciseDetailScreen (
            "Ejercicio 1",
            "Descripción del ejercicio",
            "5 min.", "Easy",
            "Instrucciones detalladas del ejercicio",
            "10",
            android.R.drawable.ic_menu_gallery
        )
    }
}

