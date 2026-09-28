package com.booktracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val QUICK_COLORS = listOf(
    0xFFE53935, 0xFFD81B60, 0xFF8E24AA, 0xFF5E35B1, 0xFF3949AB, 0xFF1E88E5, 0xFF00ACC1,
    0xFF00897B, 0xFF43A047, 0xFF7CB342, 0xFFFDD835, 0xFFFB8C00, 0xFF6D4C41, 0xFF546E7A,
)

/**
 * A colour wheel (hue around the ring, saturation towards the edge) with a
 * brightness slider, quick picks and a hex field. Calls [onChange] with ARGB.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorWheelPicker(color: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val hsv = remember(color) { FloatArray(3).also { android.graphics.Color.colorToHSV(color, it) } }
    var hue by remember(color) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(color) { mutableFloatStateOf(hsv[1]) }
    var value by remember(color) { mutableFloatStateOf(hsv[2].coerceAtLeast(0.15f)) }
    var hex by remember(color) { mutableStateOf("%06X".format(color and 0xFFFFFF)) }
    fun emit() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        hex = "%06X".format(argb and 0xFFFFFF)
        onChange(argb)
    }

    Column(modifier) {
        Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Canvas(
                Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        fun pick(p: Offset) {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val r = min(size.width, size.height) / 2f
                            val d = p - c
                            hue = ((Math.toDegrees(atan2(d.y, d.x).toDouble()) + 360) % 360).toFloat()
                            sat = (hypot(d.x, d.y) / r).coerceIn(0f, 1f)
                            emit()
                        }
                        detectTapGestures { pick(it) }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val r = min(size.width, size.height) / 2f
                            val d = change.position - c
                            hue = ((Math.toDegrees(atan2(d.y, d.x).toDouble()) + 360) % 360).toFloat()
                            sat = (hypot(d.x, d.y) / r).coerceIn(0f, 1f)
                            emit()
                        }
                    },
            ) {
                val r = size.minDimension / 2f
                val hues = (0..12).map { Color.hsv((it * 30f) % 360f, 1f, value) }
                drawCircle(Brush.sweepGradient(hues, center), r)
                drawCircle(Brush.radialGradient(listOf(Color.hsv(0f, 0f, value), Color.Transparent), center, r), r)
                val angle = Math.toRadians(hue.toDouble())
                val knob = Offset(center.x + (cos(angle) * sat * r).toFloat(), center.y + (sin(angle) * sat * r).toFloat())
                drawCircle(Color.hsv(hue, sat, value), 14.dp.toPx(), knob)
                drawCircle(Color.White, 14.dp.toPx(), knob, style = Stroke(4.dp.toPx()))
                drawCircle(Color.Black.copy(alpha = 0.3f), 16.dp.toPx(), knob, style = Stroke(1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Brightness", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Color.Black, Color.hsv(hue, sat, 1f))))
                .pointerInput(Unit) {
                    detectTapGestures { value = (it.x / size.width).coerceIn(0.15f, 1f); emit() }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> value = (change.position.x / size.width).coerceIn(0.15f, 1f); emit() }
                },
        ) {
            Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                val x = size.width * value
                drawCircle(Color.White, size.height / 2.4f, Offset(x.coerceIn(size.height / 2f, size.width - size.height / 2f), size.height / 2f), style = Stroke(4.dp.toPx()))
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QUICK_COLORS.forEach { c ->
                val argb = c.toInt()
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(if ((color or 0xFF000000.toInt()) == argb) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { onChange(argb) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(color)))
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = hex,
                onValueChange = { v ->
                    hex = v.uppercase().filter { it.isDigit() || it in 'A'..'F' }.take(6)
                    if (hex.length == 6) onChange((0xFF000000 or hex.toLong(16)).toInt())
                },
                prefix = { Text("#") },
                label = { Text("Hex colour") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
