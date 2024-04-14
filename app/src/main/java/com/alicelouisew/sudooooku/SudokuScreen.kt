package com.alicelouisew.sudooooku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.alicelouisew.sudooooku.ui.theme.SudooookuTheme
import kotlinx.coroutines.*

class SudokuScreen : ComponentActivity() {
    private var sudokuPuzzle by mutableStateOf<Array<Array<Int>>?>(null)
    private var showDialog by mutableStateOf(false)
    private var selectedCell by mutableStateOf(Pair(-1, -1))
    private var selectedNumber by mutableStateOf(1) // Added this line
    private var timerJob: Job? = null
    private var elapsedTime by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudooookuTheme {
                if (sudokuPuzzle == null) {
                    startTimer()
                }

                if (showDialog) {
                    NumberInputDialog(selectedNumber) { number ->
                        if (selectedCell.first >= 0 && selectedCell.second >= 0) {
                            sudokuPuzzle?.let {
                                it[selectedCell.first][selectedCell.second] = number
                            }
                        }
                        showDialog = false
                    }
                }
                SudokuBoard(sudokuPuzzle ?: generateSudokuPuzzle()) { i, j ->
                    selectedCell = Pair(i, j)
                    showDialog = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    // Start timer coroutine
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = MainScope().launch {
            while (true) {
                delay(1000)
                elapsedTime++
            }
        }
    }

    // Stop timer coroutine
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }


    @Composable
    fun SudokuBoard(sudoku: Array<Array<Int>>, onCellClicked: (Int, Int) -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Time: ${elapsedTime / 60}:${String.format("%02d", elapsedTime % 60)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Column {
                Spacer(modifier = Modifier.height(32.dp))

                for (i in 0 until 9) {
                    if (i % 3 == 0 && i != 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row {
                        for (j in 0 until 9) {
                            if (j % 3 == 0 && j != 0) {
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            SudokuCell(
                                number = sudoku[i][j],
                                onClick = { onCellClicked(i, j) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    for (num in 1..9) {
                        NumberButton(number = num, onNumberSelected = {
                            if (it in 1..9) {
                                onCellClicked(-1, -1)
                                showDialog = true
                                selectedNumber = it
                            }
                        })
                    }
                }
            }
        }
    }

    @Composable
    fun SudokuCell(number: Int, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(0.5.dp, Color.Black)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (number > 0) number.toString() else "",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    @Composable
    fun NumberButton(number: Int, onNumberSelected: (Int) -> Unit) {
        Button(
            onClick = { onNumberSelected(number) },
            modifier = Modifier
                .padding(1.dp)
                .size(30.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(number.toString())
        }
    }

    @Composable
    fun NumberInputDialog(selectedNumber: Int, onNumberSelected: (Int) -> Unit) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Select Number") },
            text = {
                Column {
                    Button(onClick = { onNumberSelected(selectedNumber) }) {
                        Text(selectedNumber.toString())
                    }
                }
            },
            confirmButton = {
                Button(onClick = { }) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun PreviewSudokuBoard() {
        SudooookuTheme {
            val sampleSudoku = Array(9) { Array(9) { 0 } }
            sampleSudoku[0][0] = 5
            sampleSudoku[1][2] = 6
            sampleSudoku[2][4] = 1
            sampleSudoku[4][4] = 7
            sampleSudoku[5][7] = 3
            sampleSudoku[6][8] = 9
            sampleSudoku[7][6] = 2
            sampleSudoku[8][3] = 8

            SudokuBoard(sampleSudoku) { _, _ ->
                // No operation; this is just for previewing the layout.
            }
        }
    }
}

fun generateSudokuPuzzle(): Array<Array<Int>> {
    val sudoku = Array(9) { Array(9) { 0 } }
    solveSudoku(sudoku)
    return sudoku
}

private fun solveSudoku(sudoku: Array<Array<Int>>): Boolean {
    for (row in 0 until 9) {
        for (col in 0 until 9) {
            if (sudoku[row][col] == 0) {
                for (num in 1..9) {
                    if (isNumberValid(sudoku, row, col, num)) {
                        sudoku[row][col] = num
                        if (solveSudoku(sudoku)) {
                            return true
                        }
                        sudoku[row][col] = 0
                    }
                }
                return false
            }
        }
    }
    return true
}

private fun isNumberValid(sudoku: Array<Array<Int>>, row: Int, col: Int, number: Int): Boolean {
    // Check row
    for (j in 0 until 9) {
        if (sudoku[row][j] == number) return false
    }
    // Check column
    for (i in 0 until 9) {
        if (sudoku[i][col] == number) return false
    }
    // Check 3x3 block
    val startRow = row / 3 * 3
    val startCol = col / 3 * 3
    for (i in startRow until startRow + 3) {
        for (j in startCol until startCol + 3) {
            if (sudoku[i][j] == number) return false
        }
    }
    return true
}
