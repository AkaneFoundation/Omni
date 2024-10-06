package uk.akane.omni.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import uk.akane.omni.logic.dpToPx
import com.google.android.material.color.MaterialColors
import uk.akane.omni.R

class RulerViewInch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintText = Paint().apply {
        color = MaterialColors.getColor(this@RulerViewInch, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)
        typeface = resources.getFont(R.font.hgm)
        isAntiAlias = true
    }

    private val paintMain = Paint().apply {
        color = MaterialColors.getColor(this@RulerViewInch, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    private val paintSide = Paint().apply {
        color = MaterialColors.getColor(this@RulerViewInch, com.google.android.material.R.attr.colorOutline)
        alpha = 127
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    // Calculate 1 inch in pixels
    private val inchToPx: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1f, resources.displayMetrics)
    private val inchInterval: Float = inchToPx / 10f // 1 inch is divided into 10 intervals
    private val inchTextInterval: Int = 10 // Show text for every 10 intervals (1 inch)
    private val topPadding: Float = 24f.dpToPx(context)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val numInches = (height - topPadding) / inchToPx
        val numIntervals = numInches * 10

        val longLineLength = width * 0.53f
        val midLineLength = width * 0.43f
        val shortLineLength = width * 0.34f

        for (i in 0..numIntervals.toInt()) {
            val y = topPadding + i * inchInterval
            when {
                i % inchTextInterval == 0 -> {
                    // Draw longer lines and numbers for every inch
                    canvas.drawLine(0f, y, longLineLength, y, paintMain)
                    val text = (i / inchTextInterval).toString()
                    val textWidth = paintText.measureText(text)
                    val textHeight = paintText.descent() - paintText.ascent()
                    val textX = (width - longLineLength) / 2 + longLineLength - textWidth / 2

                    val textY = y + textHeight / 3
                    paintText.color = MaterialColors.getColor(this@RulerViewInch,
                        if ((i / inchTextInterval) % 12 == 0)
                            com.google.android.material.R.attr.colorOnSurface
                        else
                            com.google.android.material.R.attr.colorOutline
                    )
                    canvas.drawText(text, textX, textY, paintText)
                }
                i % 5 == 0 -> {
                    // Draw medium lines for every 5 intervals (0.5 inch)
                    canvas.drawLine(0f, y, midLineLength, y, paintSide)
                }
                else -> {
                    // Draw shorter lines for other intervals
                    canvas.drawLine(0f, y, shortLineLength, y, paintSide)
                }
            }
        }
    }
}
