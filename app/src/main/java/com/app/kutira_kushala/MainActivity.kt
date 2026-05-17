package com.app.kutira_kushala

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.app.kutira_kushala.ui.*
import com.app.kutira_kushala.ui.theme.KutiraKushalaTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        enableEdgeToEdge()

        setContent {
            KutiraKushalaTheme {
                AppWithSplash()
            }
        }
    }
}

@Composable
fun AppWithSplash() {

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen()

        LaunchedEffect(Unit) {
            delay(2000) // 2 seconds splash
            showSplash = false
        }

    } else {
        AuthApp()
    }
}

@Composable
fun AuthApp() {

    val navController = rememberNavController()

    val startDestination =
        if (Firebase.auth.currentUser != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("login") {
            LoginScreen(navController)
        }

        composable("signup") {
            SignupScreen(navController)
        }

        composable("home") {
            HomeScreen(navController)
        }

        composable(
            route = "seller_details/{sellerId}",
            arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            SellerDetailsScreen(sellerId, navController)
        }
    }
}