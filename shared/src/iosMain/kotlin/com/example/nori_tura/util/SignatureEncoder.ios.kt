package com.example.nori_tura.util

import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.CoreGraphics.CGContextAddLineToPoint
import platform.CoreGraphics.CGContextBeginPath
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextSetLineCap
import platform.CoreGraphics.CGContextSetLineJoin
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextStrokePath
import platform.CoreGraphics.CGLineCap
import platform.CoreGraphics.CGLineJoin
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContext
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetCurrentContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIRectFill

private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

private fun ByteArray.toBase64(): String {
    val result = StringBuilder()
    var i = 0
    while (i < size) {
        val b1 = this[i].toInt() and 0xFF
        val b2 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
        val b3 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0

        result.append(BASE64_ALPHABET[(b1 shr 2)])
        result.append(BASE64_ALPHABET[((b1 and 0x03) shl 4) or (b2 shr 4)])
        if (i + 1 < size) {
            result.append(BASE64_ALPHABET[((b2 and 0x0F) shl 2) or (b3 shr 6)])
        } else {
            result.append('=')
        }
        if (i + 2 < size) {
            result.append(BASE64_ALPHABET[b3 and 0x3F])
        } else {
            result.append('=')
        }
        i += 3
    }
    return result.toString()
}

@OptIn(ExperimentalForeignApi::class)
actual fun encodeSignatureToPngBase64(paths: List<List<Offset>>, width: Int, height: Int): String {
    if (paths.isEmpty() || paths.all { it.size < 2 }) {
        return ""
    }

    UIGraphicsBeginImageContext(CGSizeMake(width.toDouble(), height.toDouble()))
    val context = UIGraphicsGetCurrentContext()
        ?: run {
            UIGraphicsEndImageContext()
            return ""
        }

    UIColor.whiteColor.setFill()
    UIRectFill(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))

    UIColor.blackColor.setStroke()
    CGContextSetLineWidth(context, 4.0)
    CGContextSetLineCap(context, 1u as CGLineCap)
    CGContextSetLineJoin(context, 1u as CGLineJoin)

    paths.forEach { stroke ->
        if (stroke.size < 2) return@forEach
        CGContextBeginPath(context)
        CGContextMoveToPoint(context, stroke.first().x.toDouble(), stroke.first().y.toDouble())
        stroke.drop(1).forEach { point ->
            CGContextAddLineToPoint(context, point.x.toDouble(), point.y.toDouble())
        }
        CGContextStrokePath(context)
    }

    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    if (image == null) return ""

    val pngData = UIImagePNGRepresentation(image) ?: return ""
    val bytes = pngData.bytes?.readBytes(pngData.length.toInt()) ?: return ""
    val base64 = bytes.toBase64()
    return "data:image/png;base64,$base64"
}
