package ru.surfstudio.shadow_layout

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas

/**
 * Размывает Bitmap c использованием [BlurUtil]
 */
fun Bitmap.stackBlur(blurRadius: Int = 1, canReuseBitmap: Boolean = false): Bitmap {
    return BlurUtil.stackBlur(this, blurRadius, canReuseBitmap)
}

fun Bitmap.renderscriptBlur(context: Context, radius: Int): Bitmap {
    return BlurUtil.renderScriptBlur(context, this, radius)
}

/**
 * Создает белые поля у Bitmap
 *
 * @param padding - величина отступа
 */
fun Bitmap.setPadding(padding: Int): Bitmap {
    return setPadding(padding, padding, padding, padding)
}

/**
 * Создает белые поля у Bitmap, позволяя настроить величину отступа для каждой из сторон
 */
fun Bitmap.setPadding(
    leftPadding: Int = 0,
    rightPadding: Int = 0,
    topPadding: Int = 0,
    bottomPadding: Int = 0
): Bitmap {
    if (leftPadding == 0 && rightPadding == 0 && topPadding == 0 && bottomPadding == 0) return this

    val outputBitmap = Bitmap.createBitmap(
        width + leftPadding + rightPadding,
        height + topPadding + bottomPadding,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(outputBitmap)

    canvas.drawBitmap(this, leftPadding.toFloat(), topPadding.toFloat(), null)
    return outputBitmap
}
