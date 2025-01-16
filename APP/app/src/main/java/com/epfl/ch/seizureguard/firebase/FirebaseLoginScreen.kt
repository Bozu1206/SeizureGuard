package com.epfl.ch.seizureguard.firebase

import android.widget.Space
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.epfl.ch.seizureguard.R
import com.epfl.ch.seizureguard.profile.PasswordTextField
import com.epfl.ch.seizureguard.profile.ProfileTextField
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun FirebaseLoginScreen(
    profileViewModel: ProfileViewModel, 
    onLoggedIn: () -> Unit,
    onBackToOnboarding: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onBackToOnboarding,
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black, CircleShape)
                .align(Alignment.TopStart)
                .clip(CircleShape),

        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back to onboarding",
                tint = Color.White
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.login_bg),
                contentDescription = "Login",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top=16.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                ProfileTextField(value = email, onValueChange = { email = it }, label = "Email")
                Spacer(modifier = Modifier.size(8.dp))
                PasswordTextField(password = pwd, onValueChange = { pwd = it })
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    if (email.isEmpty() || pwd.isEmpty()) {
                        Toast.makeText(
                            context,
                            "Please fill in all fields",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    profileViewModel.loadProfileFromEmail(email, pwd, onLoggedIn)
                    profileViewModel.retrieveAndStoreFcmToken()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
            ) {
                Text(
                    text = "Login",
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }

}