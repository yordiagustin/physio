package com.core.physio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.core.physio.ui.theme.PhysioTheme

val PurpleColor = Color(0xFF7A52E1)

class VerifyCodeActivity : ComponentActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhysioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VerifyCodeScreen()
                }
            }
        }
    }
}

@Composable
fun VerifyCodeScreen() {
    var otpValue by remember { mutableStateOf(List(4) { "" }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        Spacer(modifier = Modifier.height(100.dp))

        Image(
            painter = painterResource(id = R.drawable.verify_code),
            contentDescription = "Verification Illustration",
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text (
            text = "Verificación",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ingresa el código que se envío por SMS a tu número telefónico.",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(25.dp))

        OtpInputFields(
            otpValue = otpValue,
            onOtpValueChanged = { index, value ->
                if (value.length <= 1 && value.all { it.isDigit() }) {
                    val newList = otpValue.toMutableList()
                    newList[index] = value
                    otpValue = newList
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No compartas el código con nadie más.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val code = otpValue.joinToString("")
                println("Código ingresado: $code")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleColor
            )
        ) {
            Text(
                text = "Continuar",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Text(
                text = "No recibiste ningún código?",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Text(
                text = "  Reenviar",
                color = PurpleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    println("Registro")
                }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun OtpInputFields(
    otpValue: List<String>,
    onOtpValueChanged: (index: Int, value: String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        otpValue.forEachIndexed { index, value ->
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    onOtpValueChanged(index, newValue)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword
                ),
                singleLine = true,
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = Color.Gray.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    color = Color.Black
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VerifyCodeScreenPreview(){
    PhysioTheme {
        VerifyCodeScreen()
    }
}