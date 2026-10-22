package com.example.tradingapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            goToMain()
            return
        }

        setContent {
            LoginScreen(
                onLogin = { _, _ ->
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    goToMain()
                },
                onRegister = { _, _, _ ->
                    prefs.edit().putBoolean("is_logged_in", true).apply()
                    goToMain()
                }
            )
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }

    val primary = Color(0xFF6C63FF)
    val bgTop = Color(0xFF1E1B4B)
    val bgBottom = Color(0xFF0F0E1A)
    val fieldBg = Color(0xFF232135)
    val fieldBorder = Color(0xFF3A3757)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgTop, bgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLogin) "مرحباً بعودتك 👋" else "إنشاء حساب جديد ✨",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2740), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                TabButton("تسجيل الدخول", isLogin, Modifier.weight(1f)) { isLogin = true }
                TabButton("إنشاء حساب", !isLogin, Modifier.weight(1f)) { isLogin = false }
            }

            Spacer(Modifier.height(24.dp))

            if (!isLogin) {
                CustomField("الاسم", name) { name = it }
                Spacer(Modifier.height(14.dp))
            }

            CustomField("البريد الإلكتروني", email, KeyboardType.Email) { email = it }
            Spacer(Modifier.height(14.dp))
            CustomPasswordField("كلمة المرور", password) { password = it }

            if (!isLogin) {
                Spacer(Modifier.height(14.dp))
                CustomPasswordField("تأكيد كلمة المرور", confirm) { confirm = it }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = primary)
                )
                Text("تذكرني", color = Color(0xFFB0AEC9), fontSize = 14.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isLogin) onLogin(email, password)
                    else onRegister(name, email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Text(
                    text = if (isLogin) "دخول" else "إنشاء الحساب",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TabButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                if (active) Color(0xFF6C63FF) else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) Color.White else Color(0xFFB0AEC9),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun CustomField(
    hint: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint, color = Color(0xFF9A97B8)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF6C63FF),
            unfocusedBorderColor = Color(0xFF3A3757),
            focusedContainerColor = Color(0xFF232135),
            unfocusedContainerColor = Color(0xFF232135)
        )
    )
}

@Composable
fun CustomPasswordField(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint, color = Color(0xFF9A97B8)) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0xFF9A97B8)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF6C63FF),
            unfocusedBorderColor = Color(0xFF3A3757),
            focusedContainerColor = Color(0xFF232135),
            unfocusedContainerColor = Color(0xFF232135)
        )
    )
}

