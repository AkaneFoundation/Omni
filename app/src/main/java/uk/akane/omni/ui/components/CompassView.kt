package uk.akane.omni.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import uk.akane.omni.R
import kotlin.math.absoluteValue

class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val directionTextViews: List<TextView>
    private var degreeIndicatorTextView: TextView

    init {
        inflate(context, R.layout.compass_layout, this)
        degreeIndicatorTextView = findViewById(R.id.degree_indicator)
        directionTextViews = listOf(
            findViewById(R.id.north), findViewById(R.id.east), findViewById(R.id.south), findViewById(R.id.west),
            findViewById(R.id.direction_1), findViewById(R.id.direction_2), findViewById(R.id.direction_3),
            findViewById(R.id.direction_4), findViewById(R.id.direction_5), findViewById(R.id.direction_6),
            findViewById(R.id.direction_7), findViewById(R.id.direction_8), findViewById(R.id.direction_9),
            findViewById(R.id.direction_10), findViewById(R.id.direction_11), findViewById(R.id.direction_12),
            degreeIndicatorTextView
        )
    }

    @SuppressLint("StringFormatMatches")
    fun rotate(degree: Float) {
        this.rotation = degree
        degreeIndicatorTextView.text =
            context.getString(R.string.degree_format, degree.toInt().absoluteValue)
        directionTextViews.forEach { it.rotation = -degree }
    }

}
