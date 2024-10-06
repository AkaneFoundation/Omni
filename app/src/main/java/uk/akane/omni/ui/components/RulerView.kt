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

class RulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintText = Paint().apply {
        color = MaterialColors.getColor(this@RulerView, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics)
        typeface = resources.getFont(R.font.hgm)
        isAntiAlias = true
    }

    private val paintMain = Paint().apply {
        color = MaterialColors.getColor(this@RulerView, com.google.android.material.R.attr.colorOutline)
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    private val paintSide = Paint().apply {
        color = MaterialColors.getColor(this@RulerView, com.google.android.material.R.attr.colorOutline)
        alpha = 127
        strokeWidth = 2f.dpToPx(context)
        isAntiAlias = true
    }

    // Calculate 1mm in pixels
    private val mmToPx: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, 1f, resources.displayMetrics)
    private val topPadding: Float = 24f.dpToPx(context)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val numDivisions = ((height - topPadding) / mmToPx).toInt()
        val longLineLength = width * 0.53f
        val midLineLength = width * 0.43f
        val shortLineLength = width * 0.34f

        for (i in 0..numDivisions) {
            val y = topPadding + i * mmToPx
            when {
                i % 10 == 0 -> {
                    // Draw longer lines and numbers for every 10mm (1cm)
                    canvas.drawLine(width - longLineLength, y, width, y, paintMain)
                    val text = (i / 10).toString()
                    val textWidth = paintText.measureText(text)
                    val textHeight = paintText.descent() - paintText.ascent()
                    val textX = (width - longLineLength) / 2 - textWidth / 2
                    val textY = y + textHeight / 3
                    paintText.color = MaterialColors.getColor(this@RulerView,
                        if ((i / 10) % 5 == 0)
                            com.google.android.material.R.attr.colorOnSurface
                        else
                            com.google.android.material.R.attr.colorOutline
                    )
                    canvas.drawText(text, textX, textY, paintText)
                }
                i % 5 == 0 -> {
                    // Draw medium lines for every 5mm (0.5cm)
                    canvas.drawLine(width - midLineLength, y, width, y, paintSide)
                }
                else -> {
                    // Draw shorter lines for other millimeters
                    canvas.drawLine(width - shortLineLength, y, width, y, paintSide)
                }
            }
        }
    }
}
