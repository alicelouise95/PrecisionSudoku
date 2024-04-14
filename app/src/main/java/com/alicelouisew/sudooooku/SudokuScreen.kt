package com.alicelouisew.sudooooku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.alicelouisew.sudooooku.ui.theme.SudooookuTheme
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    private var sudokuPuzzle by mutableStateOf<Array<Array<Int>>?>(null)
    private var selectedCell by mutableStateOf<Pair<Int, Int>?>(null)
    private var selectedNumber by mutableStateOf(1)
    private var timerJob: Job? = null
    private var elapsedTime by mutableStateOf(0)
    private var mistakesCount by mutableStateOf(0)
    private var gameOverPopupVisible by mutableStateOf(false)
    private var wrongNumberVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudooookuTheme {
                if (sudokuPuzzle == null) {
                    startTimer()
                    sudokuPuzzle = generateSudokuPuzzle()
                }

                SudokuBoard(
                    sudoku = sudokuPuzzle!!,
                    selectedCell = selectedCell,
                    onCellClicked = { i, j ->
                        selectedCell = Pair(i, j)
                    },
                    onNumberSelected = { number ->
                        selectedCell?.let { (i, j) ->
                            val isValid = isNumberValid(sudokuPuzzle!!, i, j, number)
                            if (isValid) {
                                sudokuPuzzle?.let {
                                    it[i][j] = number
                                    selectedCell = null
                                    wrongNumberVisible = false
                                }
                            } else {
                                selectedCell = null
                                wrongNumberVisible = true
                                mistakesCount++
                                if (mistakesCount >= 3) {
                                    gameOverPopupVisible = true
                                } else {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(2000)
                                        wrongNumberVisible = false
                                    }
                                }
                            }
                        }
                    }
                )

                if (gameOverPopupVisible) {
                    GameOverPopup {
                        sudokuPuzzle = generateSudokuPuzzle()
                        mistakesCount = 0
                        gameOverPopupVisible = false
                        wrongNumberVisible = false // Reset the wrong number message visibility
                    }
                }

                MistakesCount(mistakesCount)
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
    fun SudokuBoard(
        sudoku: Array<Array<Int>>,
        selectedCell: Pair<Int, Int>?,
        onCellClicked: (Int, Int) -> Unit,
        onNumberSelected: (Int) -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Display elapsed time above the puzzle box
            Text(
                text = "Time: ${elapsedTime / 60}:${String.format("%02d", elapsedTime % 60)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 16.dp)
            )

            // Display Sudoku cells
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 30.dp) // Adjust the top padding to move the Sudoku puzzle down
            ) {
                for (i in 0 until 9) {
                    if (i % 3 == 0 && i != 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    Row {
                        for (j in 0 until 9) {
                            if (j % 3 == 0 && j != 0) {
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            val isSelected = selectedCell?.first == i && selectedCell.second == j
                            SudokuCell(
                                number = sudoku[i][j],
                                onClick = { onCellClicked(i, j) },
                                isSelected = isSelected,
                                onNumberSelected = onNumberSelected
                            )
                        }
                    }
                }
            }

            // Display number buttons below the puzzle box
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 40.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (num in 1..9) {
                    NumberButton(
                        number = num,
                        onNumberSelected = onNumberSelected
                    )
                }
            }

            // Show wrong number message
            if (wrongNumberVisible) {
                WrongNumberMessage()
            }
        }
    }

    @Composable
    fun SudokuCell(
        number: Int,
        onClick: () -> Unit,
        isSelected: Boolean,
        onNumberSelected: (Int) -> Unit
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(0.dp, if (isSelected) Color.Blue else Color.Black)
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
    fun WrongNumberMessage() {
        val shakeOffset = rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .offset(x = shakeOffset.value.dp)
            ) {
                Text(
                    text = "Wrong number!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Red
                )
            }
        }
    }

    @Composable
    fun MistakesCount(count: Int) {
        Box(
            modifier = Modifier
                .padding(end = 16.dp)
        ) {
            Text(
                text = "Mistakes: $count / 3",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp
            )
        }
    }

    private fun resetTimer() {
        elapsedTime = 0
        startTimer()
    }

    @Composable
    fun GameOverPopup(onRetry: () -> Unit) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Game Over!") },
            text = { Text("You've made 3 mistakes.") },
            confirmButton = {
                Button(
                    onClick = {
                        onRetry()
                        resetTimer() // Reset the timer when starting a new game
                    }
                ) {
                    Text("Try a new puzzle")
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    @Composable
    fun CongratsPopup() {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Congratulations!") },
            text = {
                Column {
                    Text("You completed the puzzle in ${elapsedTime / 60} minutes.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sudokuPuzzle = generateSudokuPuzzle()
                        resetTimer() // Reset the timer when starting a new game
                    }
                ) {
                    Text("Try another puzzle")
                }
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
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

            SudokuBoard(sampleSudoku, null, { _, _ -> }, { _ -> })
        }
    }
}

fun generateSudokuPuzzle(): Array<Array<Int>> {
    val sudoku = Array(9) { Array(9) { 0 } }
    val solution = Array(9) { Array(9) { 0 } }

    // Generate a valid solution
    solveSudoku(solution)

    // Copy the solution to the sudoku grid
    for (i in 0 until 9) {
        for (j in 0 until 9) {
            sudoku[i][j] = solution[i][j]
        }
    }

    // Remove numbers randomly while ensuring the puzzle remains solvable
    removeNumbersFromSudoku(sudoku, 40) // Adjust the number of cells to remove as needed

    return sudoku
}

fun isPuzzleCompleted(sudoku: Array<Array<Int>>): Boolean {
    for (row in sudoku) {
        for (cell in row) {
            if (cell == 0) {
                return false
            }
        }
    }
    return true
}

fun removeNumbersFromSudoku(sudoku: Array<Array<Int>>, cellsToRemove: Int) {
    val random = java.util.Random()
    var count = 0
    while (count < cellsToRemove) {
        val row = random.nextInt(9)
        val col = random.nextInt(9)
        if (sudoku[row][col] != 0) {
            sudoku[row][col] = 0
            count++
        }
    }
}

fun solveSudoku(sudoku: Array<Array<Int>>): Boolean {
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

fun isNumberValid(sudoku: Array<Array<Int>>, row: Int, col: Int, number: Int): Boolean {
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
