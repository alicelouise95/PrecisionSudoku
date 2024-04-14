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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.alicelouisew.sudooooku.ui.theme.SudooookuTheme
import kotlinx.coroutines.*
import java.util.Random

class MainActivity : ComponentActivity() {
    private var sudokuPuzzle by mutableStateOf<Array<Array<Int>>?>(null)
    private var selectedCell by mutableStateOf<Pair<Int, Int>?>(null)
    private var selectedNumber by mutableStateOf(1)
    private var timerJob: Job? = null
    private var elapsedTime by mutableStateOf(0)
    private var mistakesCount by mutableStateOf(0)
    private var gameOverPopupVisible by mutableStateOf(false)
    private var wrongNumberVisible by mutableStateOf(false)
    private var puzzleCompleted by mutableStateOf(false)

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
                                    if (isPuzzleCompleted(it)) {
                                        puzzleCompleted = true
                                    }
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

                if (puzzleCompleted) {
                    CongratsPopup {
                        sudokuPuzzle = generateSudokuPuzzle()
                        resetTimer() // Reset the timer when starting a new game
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
                .padding(20.dp)
        ) {
            // Display elapsed time above the puzzle box
            Text(
                text = "Time: ${elapsedTime / 60}:${String.format("%02d", elapsedTime % 60)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )

            // Display Sudoku cells
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 130.dp) // Adjusted top padding
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
                    .padding(top = 280.dp),
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
                .border(1.5.dp, if (isSelected) Color.Blue else Color.Black)
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
                .padding(end = 16.dp, top = 60.dp)
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
        if (mistakesCount >= 3) {
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
    }

    @Composable
    fun CongratsPopup(onRetry: () -> Unit) {
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
                        onRetry()
                        resetTimer() // Reset the timer when starting a new game
                        puzzleCompleted = false // Reset the puzzleCompleted state
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

    // Fill the Sudoku puzzle with valid numbers
    fillSudoku(sudoku)

    // Remove numbers randomly while ensuring the puzzle remains solvable
    val random = Random()
    removeNumbersFromSudoku(sudoku, random.nextInt(50) + 20) // Adjust the range of cells to remove as needed

    return sudoku
}

fun fillSudoku(sudoku: Array<Array<Int>>) {
    solveSudoku(sudoku) // Start with a complete solution
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
    val emptyCell = findEmptyCell(sudoku)
    if (emptyCell == null) {
        // If no empty cell is found, the puzzle is solved
        return true
    }

    val (row, col) = emptyCell
    val numbers = (1..9).shuffled() // Shuffle the numbers to insert randomly

    for (number in numbers) {
        if (isNumberValid(sudoku, row, col, number)) {
            sudoku[row][col] = number
            if (solveSudoku(sudoku)) {
                return true
            }
            sudoku[row][col] = 0
        }
    }
    return false
}

fun findEmptyCell(sudoku: Array<Array<Int>>): Pair<Int, Int>? {
    for (i in 0 until 9) {
        for (j in 0 until 9) {
            if (sudoku[i][j] == 0) {
                return Pair(i, j)
            }
        }
    }
    return null
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