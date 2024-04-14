package com.alicelouisew.sudooooku

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.alicelouisew.sudooooku.ui.theme.SudooookuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudooookuTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "opening_screen") {
                    composable("opening_screen") { OpeningScreen(navController) }
                    composable("sudoku_screen") { SudokuScreen() }
                }
            }
        }
    }
}

@Composable
fun OpeningScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = { navController.navigate("sudoku_screen") },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Start new puzzle")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SudooookuTheme {
        OpeningScreen(navController = rememberNavController())
    }
}
