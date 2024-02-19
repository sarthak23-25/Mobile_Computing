package com.example.mc_a1_2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mc_a1_2.ui.theme.MC_A1_2Theme
import kotlin.math.round

class MainActivity : ComponentActivity() {
    var valueProgress by mutableStateOf(0)
    var currentItemIndex by mutableStateOf(0)
    var visitedItems by mutableStateOf(List(17) { false })

    val listItem = listOf(
        "IIIT", "Govind Puri", "Kalkaji Mandir", "Nehru Place", "Kailash Colony",
        "Moolchand", "Lajpat Nagar", "Jangpura", "Stadium", "Khan Market",
        "Central Secretariat", "Patel Chowk", "Rajiv Chowk", "New Delhi", "Shivaji Stadium",
        "Dhaula Kuan", "Delhi Aerocity"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MC_A1_2Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(26.dp))
                        ProgressIndict(valueProgress, listItem
                        ) {
                            Toast.makeText(this@MainActivity, "Distance covered!", Toast.LENGTH_LONG).show()
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ProgressDisplayBox(
                            currentProgress = valueProgress,
                            upcomingProgress = valueProgress + 10,
                            (listItem.size-1) * 10,
                            currentItem = listItem[currentItemIndex],
                            nextItem = if (currentItemIndex < listItem.size - 1) listItem[currentItemIndex + 1] else "End"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val distanceRemaining = (listItem.size-1) * 10 - valueProgress
                        if (distanceRemaining == 0 || valueProgress >= (listItem.size-1) * 10) {
                            NextButton(
                                text = "Reset",
                                progress = 0,
                                itemCount = listItem.size
                            ) {
                                resetValues()
                            }
                        } else {
                            NextButton(
                                text = "Next Stop",
                                progress = valueProgress + 5,
                                itemCount = listItem.size
                            ) { increasedProgress ->
                                valueProgress = increasedProgress
                                currentItemIndex++
                            }
                        }
                        CustomButtonCheckProgress(
                            text = "Check Progress",
                            progress = valueProgress,
                            itemCount = listItem.size
                        ) { progressValue ->
                            Toast.makeText(this@MainActivity, "Progress: $progressValue%", Toast.LENGTH_SHORT).show()
                        }
                        ColumnList(currentItemIndex, listItem, visitedItems)
                    }
                }
            }
        }
    }
    private fun resetValues() {
        valueProgress = 0
        currentItemIndex = 0
        visitedItems = List(17) { false }
    }
}

@Composable
fun ProgressIndict(
    progress: Int,
    listItem: List<String>,
    onReachedEnd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(18.dp)
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth()) {
            val listSize1 = listItem.size
            val totalWidth = size.width
            val progressValue = (progress / 10 + 1) / listSize1.toFloat()
            val roundedRadius = 9.dp.toPx()
            val indicatorWidth = totalWidth * progressValue

            drawRoundRect(
                color = Color.Gray,
                topLeft = Offset(0f, 0f),
                size = Size(totalWidth, size.height),
                cornerRadius = CornerRadius(roundedRadius, roundedRadius)
            )

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(0f, 0f),
                size = Size(indicatorWidth, size.height),
                cornerRadius = CornerRadius(roundedRadius, roundedRadius)
            )

            if (progressValue >= 1.0f) {
                onReachedEnd()
            }
        }
    }
}

@Composable
fun ProgressDisplayBox(
    currentProgress: Int,
    upcomingProgress: Int,
    totalDistance: Int,
    currentItem: String,
    nextItem: String
) {
    val boxColor = Color.Black
    val textColor = Color.White
    val isKilometers by remember { mutableStateOf(true) }
    LaunchedEffect(isKilometers) {
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .border(3.dp, Color.White, shape = RoundedCornerShape(16.dp))
            .background(boxColor, shape = RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Text(
            text = "Distance Covered: ${convertToUnits(currentProgress)}",
            color = textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Distance b/w Stations: ${convertToUnits(upcomingProgress-currentProgress)}",
            color = textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Distance Remaining: ${convertToUnits(totalDistance-currentProgress)}",
            color = textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Current Station: $currentItem",
            color = textColor, // Change text color
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Next Station: $nextItem",
            color = textColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CustomToggleButton(
            text = "Toggle Units",
            onClick = {
                toggleUnit()
            }
        )
    }
}

@Composable
fun CustomToggleButton(text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(text = text, color = Color.Black)
        }
    }
}

private fun convertToUnits(progress: Int): String {
    val unit = if (isKilometers) "km" else "miles"
    val convertedProgress = if (isKilometers) progress else (progress * 0.621371).toInt()
    return "$convertedProgress $unit"
}

private fun toggleUnit() {
    isKilometers = !isKilometers
}

private var isKilometers by mutableStateOf(true)

@Composable
fun NextButton(text: String, progress: Int, itemCount: Int, onClick: (Int) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Button(
            onClick = {
                val increasedProgress = progress + (100f / itemCount)
                onClick(increasedProgress.toInt())
            }
        ) {
            Text(text = text, color = Color.Black)
        }
    }
}

@Composable
fun CustomButtonCheckProgress(
    text: String,
    progress: Int,
    itemCount: Int,
    onClick: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
    ) {
        Button(
            onClick = {
                val progressPercentage = ((progress.toFloat()+10) / ((itemCount) * 10).toFloat()) * 100f
                val roundedProgressPercentage = round(progressPercentage * 100) / 100
                onClick(roundedProgressPercentage) // Pass the increased progress to the click handler
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text(text = text, color = Color.Black)
        }
    }
}

@Composable
fun ColumnList(currentItemIndex: Int, listItem: List<String>, visitedItems: List<Boolean>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .border(3.dp, Color.White, shape = RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.Center)
        ) {
            if (listItem.size > 10) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    itemsIndexed(listItem) { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.6.dp)
                        ) {
                            if (index < currentItemIndex && !visitedItems[index]) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Visited Item",
                                    tint = Color.Red,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                            }
                            if (index == currentItemIndex && !visitedItems[index]) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Current Item",
                                    tint = Color.Yellow,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                            }
                            if (index > currentItemIndex && !visitedItems[index]) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Upcoming Item",
                                    tint = Color.Green,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                            }
                            Text(text = item, style = TextStyle(fontSize = 15.sp))
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    listItem.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.6.dp)
                        ) {
                            if (index < currentItemIndex && !visitedItems[index]) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Visited Item",
                                    tint = Color.Red,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                            }
                            if (index == currentItemIndex && !visitedItems[index]) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Current Item",
                                    tint = Color.Yellow,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                            }
                            if (index > currentItemIndex && !visitedItems[index]) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Upcoming Item",
                                    tint = Color.Green,
                                    modifier = Modifier.padding(end = 5.dp)
                                )
                            }
                            Text(text = item, style = TextStyle(fontSize = 15.sp))
                        }
                    }
                }
            }
        }
    }
}
