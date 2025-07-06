package com.core.physio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.core.physio.data.model.Exercise as ApiExercise
import com.core.physio.data.repository.ExerciseRepository
import com.core.physio.ui.theme.PhysioTheme
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable

class HomeActivity : ComponentActivity() {
    private val exerciseRepository = ExerciseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhysioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(exerciseRepository)
                }
            }
        }
    }
}

data class Exercise(
    val title: String,
    val description: String,
    val instructions: String,
    val duration: String,
    val level: String,
    val repetitions: Int,
    val backgroundColor: Color,
    val imageRes: Int
)

@Composable
fun HomeScreen(exerciseRepository: ExerciseRepository) {
    var exercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                //ToDo: Replace with actual patient ID
                val apiExercises = exerciseRepository.getExercises("42f0ffab-ce7e-45d2-ace8-fbb83e241d0c")
                exercises = apiExercises.map { apiExercise ->
                    Exercise(
                        title = apiExercise.exerciseName,
                        description = apiExercise.description,
                        duration = "${apiExercise.estimatedDurationMinutes} min.",
                        level = when (apiExercise.difficultyLevel) {
                            1 -> "Basic"
                            2 -> "Intermediate"
                            else -> "Advanced"
                        },
                        backgroundColor = when ((apiExercise.exerciseId % 3)) {
                            0 -> Color(0xFFEAEAFE)
                            1 -> Color(0xFFE6F4F1)
                            else -> Color(0xFFFFF6E6)
                        },
                        imageRes = R.drawable.exercises,
                        instructions = apiExercise.instructions,
                        repetitions = apiExercise.repetitions
                    )
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ejercicios de hoy", style = MaterialTheme.typography.headlineMedium)
        Text("martes 10 de junio del 2025", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            exercises.forEach { exercise ->
                ExerciseCard(exercise, onClick = {
                    val intent = Intent(context, ExerciseDetailActivity::class.java).apply {
                        putExtra("title", exercise.title)
                        putExtra("description", exercise.description)
                        putExtra("duration", exercise.duration)
                        putExtra("level", exercise.level)
                        putExtra("instructions", exercise.instructions)
                        putExtra("imageRes", exercise.imageRes)
                        putExtra("repetitions", exercise.repetitions.toString())
                    }
                    context.startActivity(intent)
                })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit = {}) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(exercise.backgroundColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .padding(16.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(exercise.title, style = MaterialTheme.typography.titleMedium)
            Text(exercise.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Card(
                    modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )

                        Text(
                            exercise.duration,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))

                Card(
                    modifier = Modifier.background(Color.White, RoundedCornerShape(5.dp)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                   Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ){
                       androidx.compose.material3.Icon(
                           imageVector = androidx.compose.material.icons.Icons.Default.Star,
                           contentDescription = null,
                           tint = Color(0xFFFFD700),
                           modifier = Modifier.size(16.dp)
                       )
                       Text(exercise.level, style = MaterialTheme.typography.bodySmall)
                   }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = exercise.imageRes),
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview(){
    PhysioTheme {
        HomeScreen(ExerciseRepository())
    }
}