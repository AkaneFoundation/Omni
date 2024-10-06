package uk.akane.omni.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.res.ResourcesCompat
import uk.akane.omni.R
import uk.akane.omni.logic.dpToPx
import com.google.android.material.color.MaterialColors
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class SpiritLevelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var pitch: Float = 0f
    private var roll: Float = 0f
    private var balance: Float = 0f
    private var pitchAngle: Float = 0f

    private val containerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 42f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
        typeface = resources.getFont(R.font.hgm)
    }

    private val outerLevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    }

    private var colorPrimary: Int = 0
    private var colorOnPrimary: Int = 0
    private var colorTertiary: Int = 0
    private var colorOnTertiary: Int = 0
    private var colorSurface: Int = 0
    private var colorOnSurface: Int = 0
    private var colorPrimaryContainer: Int = 0

    private val leftPolygon = ResourcesCompat.getDrawable(resources, R.drawable.ic_polygon_left, null)!!
    private val rightPolygon = ResourcesCompat.getDrawable(resources, R.drawable.ic_polygon_right, null)!!
    private val polygonHeight = 51.dpToPx(context)
    private val polygonWidth = 50.dpToPx(context)
    private val sideMargin = 16.dpToPx(context)
    private val roundCorner = 16f.dpToPx(context)

    init {
        colorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
        colorOnPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
        colorTertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorTertiary)
        colorOnTertiary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnTertiary)
        colorSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface)
        colorOnSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface)
        colorPrimaryContainer = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer)
        containerPaint.color = colorSurface
        textPaint.color = colorOnSurface
    }

    private var levelRadius: Float = 0f
    private var translationRange: Float = 0f
    private var directionalLength: Float = 0f
    private var firstTransformDegree: Float = 0f
    private var transformFactor: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        levelRadius = w / 2 * 0.36f
        translationRange = max(w, h).toFloat()
        directionalLength = sqrt((w.toFloat().pow(2)) + (h.toFloat().pow(2))) / 2
        val topLeft = (height - polygonHeight) / 2
        leftPolygon.setBounds(
            sideMargin,
            topLeft,
            sideMargin + polygonWidth,
            topLeft + polygonHeight
        )
        rightPolygon.setBounds(
            width - sideMargin - polygonWidth,
            (height - polygonHeight) / 2,
            width - sideMargin,
            (height - polygonHeight) / 2 + polygonHeight
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Calculates indicator center location.
        val cx = width / 2f
        val cy = height / 2f

        // Calculates horizontal indicator position.
        val levelCx = cx + (roll / 90) * translationRange
        val levelCy = cy - (pitch / 90) * translationRange
        val offScreenProgress = sqrt(
            (((cy - levelCy).absoluteValue).pow(2) + ((cx - levelCx).absoluteValue).pow(2))
        ) - levelRadius

        if (pitch.absoluteValue > 2f || roll.absoluteValue > 2f) {
            levelPaint.color = colorPrimary
            outerLevelPaint.color = colorOnPrimary
        } else {
            levelPaint.color = colorTertiary
            outerLevelPaint.color = colorOnTertiary
        }

        drawHorizontalIndicators(cx, cy, levelCx, levelCy, canvas)
        drawVerticalLayer(canvas, balance, offScreenProgress)

        val saveCount = canvas.saveLayer(null, null)

        drawCenteredText(cx, cy, canvas, balance, offScreenProgress)
        drawInvertedTextLayer(levelCx, levelCy, canvas)
        drawRoundedRect(canvas, transformFactor, balance, outerLevelPaint)

        canvas.restoreToCount(saveCount)
    }

    private fun drawHorizontalIndicators(
        cx: Float,
        cy: Float,
        levelCx: Float,
        levelCy: Float,
        canvas: Canvas
    ) {
        // Draws container radius.
        val bigCircleRadius = sqrt(
            (((cy - levelCy).absoluteValue).pow(2) + ((cx - levelCx).absoluteValue).pow(2))
        ) + levelRadius

        // Draws indicators
        canvas.drawCircle(cx, cy, bigCircleRadius, containerPaint)
        canvas.drawCircle(levelCx, levelCy, levelRadius, levelPaint)
    }

    private fun drawVerticalLayer(
        canvas: Canvas,
        angle: Float,
        offScreenProgress: Float
    ) {
        if (offScreenProgress > directionalLength) {
            if (firstTransformDegree == 0f) firstTransformDegree = pitchAngle
            transformFactor = (pitchAngle - firstTransformDegree) / 5f
            if (angle.absoluteValue > 2f && angle !in 178f .. 182f) {
                leftPolygon.setTint(colorPrimaryContainer)
                rightPolygon.setTint(colorPrimaryContainer)
            } else {
                leftPolygon.setTint(colorTertiary)
                rightPolygon.setTint(colorTertiary)
            }
            if (transformFactor in 0f .. 1f) {
                leftPolygon.alpha = (transformFactor * 255).toInt()
                rightPolygon.alpha = (transformFactor * 255).toInt()
                drawRoundedRect(canvas, transformFactor, angle, levelPaint)
                containerPaint.alpha = ((1f - transformFactor) * 255).toInt()
            } else if (transformFactor > 1f) {
                leftPolygon.alpha = 255
                rightPolygon.alpha = 255
                drawRoundedRect(canvas, 1f, angle, levelPaint)
                containerPaint.alpha = 0
            }
            leftPolygon.draw(canvas)
            rightPolygon.draw(canvas)
        } else {
            return
        }
    }

    private fun drawRoundedRect(
        canvas: Canvas,
        transformValue: Float,
        angle: Float,
        paint: Paint
    ) {
        canvas.save()
        canvas.rotate(angle, width / 2f, height / 2f)
        canvas.drawRoundRect(
            0f,
            height - (height / 2 * min(transformValue, 1f)),
            width.toFloat(),
            height.toFloat() * 2,
            roundCorner,
            roundCorner,
            paint
        )
        canvas.restore()
    }

    private fun drawCenteredText(
        cx: Float,
        cy: Float,
        canvas: Canvas,
        textAngle: Float,
        offScreenProgress: Float
    ) {
        canvas.save()
        canvas.rotate(textAngle, cx, cy)
        val text =
            if (offScreenProgress < directionalLength)
                " ${pitchAngle.toInt().absoluteValue}°"
            else if (offScreenProgress > directionalLength && pitch > 0)
                " ${(- balance + 180f).toInt().absoluteValue}°"
            else
                " ${textAngle.toInt().absoluteValue}°"
        canvas.drawText(text, cx, cy + (textPaint.textSize / 4), textPaint)
        canvas.restore()
    }

    private fun drawInvertedTextLayer(
        levelCx: Float,
        levelCy: Float,
        canvas: Canvas
    ) {
        // Draws an overlay layer for the inverted text color.
        canvas.drawCircle(levelCx, levelCy, levelRadius, outerLevelPaint)
    }

    fun updatePitchAndRollAndBalance(pitch: Float, roll: Float, balance: Float) {
        this.pitch = pitch
        this.roll = roll
        this.balance = if (pitch < 0) balance else 180f - balance
        this.pitchAngle = sqrt(pitch.absoluteValue.pow(2) + roll.absoluteValue.pow(2))
        invalidate()
    }
}
