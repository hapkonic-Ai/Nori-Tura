package com.example.nori_tura.util

import androidx.compose.ui.geometry.Offset
import kotlinx.browser.document
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

actual fun encodeSignatureToPngBase64(paths: List<List<Offset>>, width: Int, height: Int): String {
    if (paths.isEmpty() || paths.all { it.size < 2 }) {
        return ""
    }

    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = width
    canvas.height = height
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    ctx.fillStyle = "white"
    ctx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
    ctx.strokeStyle = "black"
    ctx.lineWidth = 4.0

    paths.forEach { stroke ->
        if (stroke.size < 2) return@forEach
        ctx.beginPath()
        ctx.moveTo(stroke.first().x.toDouble(), stroke.first().y.toDouble())
        stroke.drop(1).forEach { ctx.lineTo(it.x.toDouble(), it.y.toDouble()) }
        ctx.stroke()
    }

    return canvas.toDataURL("image/png")
}
