package com.example.nori_tura.util

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class Stroke(val points: List<PointDto>)

@Serializable
private data class PointDto(val x: Double, val y: Double)

actual fun encodeSignatureToPngBase64(paths: List<List<Offset>>, width: Int, height: Int): String {
    if (paths.isEmpty() || paths.all { it.size < 2 }) {
        return ""
    }

    val strokes = paths
        .filter { it.size >= 2 }
        .map { stroke -> Stroke(stroke.map { PointDto(it.x.toDouble(), it.y.toDouble()) }) }

    val json = Json.encodeToString(strokes)

    return jsEncodeSignature(json, width, height)
}

private fun jsEncodeSignature(strokesJson: String, width: Int, height: Int): String =
    js("""
        (function(strokesJson, width, height) {
            var strokes = JSON.parse(strokesJson);
            var canvas = document.createElement('canvas');
            canvas.width = width;
            canvas.height = height;
            var ctx = canvas.getContext('2d');
            ctx.fillStyle = 'white';
            ctx.fillRect(0, 0, width, height);
            ctx.strokeStyle = 'black';
            ctx.lineWidth = 4;
            ctx.lineCap = 'round';
            ctx.lineJoin = 'round';
            for (var i = 0; i < strokes.length; i++) {
                var points = strokes[i].points;
                if (points.length < 2) continue;
                ctx.beginPath();
                ctx.moveTo(points[0].x, points[0].y);
                for (var j = 1; j < points.length; j++) {
                    ctx.lineTo(points[j].x, points[j].y);
                }
                ctx.stroke();
            }
            return canvas.toDataURL('image/png');
        })(strokesJson, width, height)
    """)
