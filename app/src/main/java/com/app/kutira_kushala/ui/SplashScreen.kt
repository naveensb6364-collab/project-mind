package com.app.kutira_kushala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BackHand
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kutira_kushala.ui.theme.*

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo (leaf + hands motif)
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                
                // Leaf Motif (Representing "Kutira" - Nature/Rural)
                Icon(
                    imageVector = Icons.Outlined.Eco,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .offset(y = (-25).dp),
                    tint = DeepGreen
                )
                // Hands Motif (Representing "Kushala" - Skilled Hands)
                Icon(
                    imageVector = Icons.Outlined.BackHand,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .offset(y = 35.dp),
                    tint = Saffron
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App name in English
            Text(
                text = "Kutira-Kushala",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = DeepGreen,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App name in Kannada
            Text(
                text = "ಕುಟೀರ ಕುಶಲ",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreen
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tagline centered below
            Text(
                text = "\"Skilled Hands, Big Markets\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    KutiraKushalaTheme {
        SplashScreen()
    }
}
