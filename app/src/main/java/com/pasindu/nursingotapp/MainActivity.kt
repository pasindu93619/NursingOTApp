// com/pasindu/nursingotapp/MainActivity.kt
package com.pasindu.nursingotapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.pasindu.nursingotapp.ui.navigation.AppNavigation
import com.pasindu.nursingotapp.ui.theme.NursingOTAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🛑 STRICT LIGHT THEME ENFORCEMENT
        // This overrides the system's dark mode setting entirely for this app.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContent {
            NursingOTAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // THIS IS THE FIX!
                    // We let the AppNavigation handle all the screens now!
                    AppNavigation()
                }
            }
        }
    }
}