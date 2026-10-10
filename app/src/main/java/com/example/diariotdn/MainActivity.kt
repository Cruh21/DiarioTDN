package com.example.diariotdn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.diariotdn.data.AppDatabase
import com.example.diariotdn.ui.components.AppNavigationDrawer
import com.example.diariotdn.ui.theme.DiarioTDNTheme
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModel
import com.example.diariotdn.ui.viewmodel.DailyRecordViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: DailyRecordViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        DailyRecordViewModelFactory(
            database = database,
            dao = database.dailyRecordDao(),
            loteDao = database.loteDao(),
            categoryOptionDao = database.categoryOptionDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Retiene la SplashScreen hasta que ViewModel termine la primera recolección de Room
        splashScreen.setKeepOnScreenCondition {
            viewModel.isAppLoading.value
        }

        enableEdgeToEdge()
        setContent {
            DiarioTDNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationDrawer(viewModel = viewModel)
                }
            }
        }
    }
}
