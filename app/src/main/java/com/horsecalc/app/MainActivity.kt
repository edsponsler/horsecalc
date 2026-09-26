package com.horsecalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.horsecalc.app.ui.screens.PayoutScreen
import com.horsecalc.app.ui.screens.PotIntakeScreen
import com.horsecalc.app.ui.screens.WinnersScreen
import com.horsecalc.app.ui.theme.HorseCalcTheme
import com.horsecalc.app.ui.theme.SurfaceDark
import com.horsecalc.app.ui.viewmodel.AppStep
import com.horsecalc.app.ui.viewmodel.HorseCalcViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: HorseCalcViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HorseCalcTheme {
                val uiState by viewModel.uiState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SurfaceDark
                ) { innerPadding ->
                    HorseCalcApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun HorseCalcApp(
    viewModel: HorseCalcViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.step) {
        AppStep.POT_INTAKE -> {
            PotIntakeScreen(
                state = uiState,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
        AppStep.WINNERS_SETUP -> {
            WinnersScreen(
                state = uiState,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
        AppStep.PAYOUT_SUMMARY -> {
            PayoutScreen(
                state = uiState,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
    }
}
