package com.example.nori_tura.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
    onPathsChange: (List<List<Offset>>) -> Unit = {}
) {
    var paths by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.White, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (paths.isEmpty() && currentPath.isEmpty()) {
                Text(
                    text = "Sign here",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val offset = change.position
                                when (event.type) {
                                    PointerEventType.Press -> {
                                        currentPath = listOf(offset)
                                    }

                                    PointerEventType.Move -> {
                                        if (currentPath.isNotEmpty()) {
                                            currentPath = currentPath + offset
                                        }
                                    }

                                    PointerEventType.Release -> {
                                        if (currentPath.isNotEmpty()) {
                                            paths = paths + listOf(currentPath)
                                            currentPath = emptyList()
                                            onPathsChange(paths)
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val strokesToDraw = paths + if (currentPath.isNotEmpty()) listOf(currentPath) else emptyList()
                strokesToDraw.forEach { stroke ->
                    if (stroke.size < 2) return@forEach
                    val path = Path().apply {
                        moveTo(stroke.first().x, stroke.first().y)
                        stroke.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                paths = emptyList()
                currentPath = emptyList()
                onPathsChange(emptyList())
                onClear()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Clear")
        }
    }
}
