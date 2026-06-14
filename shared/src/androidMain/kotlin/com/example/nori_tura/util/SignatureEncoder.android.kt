package com.example.nori_tura.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream

actual fun encodeSignatureToPngBase64(paths: List<List<Offset>>, width: Int, height: Int): String {
    if (paths.isEmpty() || paths.all { it.size < 2 }) {
        return ""
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    paths.forEach { stroke ->
        if (stroke.size < 2) return@forEach
        val path = Path().apply {
            moveTo(stroke.first().x, stroke.first().y)
            stroke.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, paint)
    }

    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val bytes = stream.toByteArray()
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:image/png;base64,$base64"
}
