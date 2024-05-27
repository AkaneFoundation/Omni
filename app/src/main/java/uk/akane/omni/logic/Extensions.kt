package uk.akane.omni.logic

import android.animation.TimeInterpolator
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Interpolator
import android.hardware.SensorManager
import androidx.core.graphics.Insets
import android.os.Build
import android.os.Looper
import android.os.StrictMode
import android.view.Display
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import uk.akane.omni.BuildConfig

@Suppress("NOTHING_TO_INLINE")
inline fun Context.doIHavePermission(perm: String) =
	ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED

val Context.isLocationPermissionGranted: Boolean
	get() = doIHavePermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
			|| doIHavePermission(android.Manifest.permission.ACCESS_FINE_LOCATION)

fun ComponentActivity.enableEdgeToEdgeProperly() {
	if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
		Configuration.UI_MODE_NIGHT_YES
	) {
		enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
	} else {
		val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
		enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, darkScrim))
	}
}

fun View.fadOutAnimation(
	duration: Long = 300,
	visibility: Int = View.INVISIBLE,
	interpolator: TimeInterpolator,
	completion: (() -> Unit)? = null
) {
	animate()
		.alpha(0f)
		.setDuration(duration)
		.setInterpolator(interpolator)
		.withEndAction {
			this.visibility = visibility
			completion?.let {
				it()
			}
		}
}

fun View.fadInAnimation(
	duration: Long = 300,
	completion: (() -> Unit)? = null,
	interpolator: TimeInterpolator
) {
	alpha = 0f
	visibility = View.VISIBLE
	animate()
		.alpha(1f)
		.setDuration(duration)
		.setInterpolator(interpolator)
		.withEndAction {
			completion?.let {
				it()
			}
		}
}

fun TextView.setTextAnimation(
	text: CharSequence?,
	duration: Long = 300,
	completion: (() -> Unit)? = null,
	skipAnimation: Boolean = false,
	fadeInInterpolator: TimeInterpolator,
	fadeOutInterpolator: TimeInterpolator
) {
	if (skipAnimation) {
		this.text = text
		completion?.let { it() }
	} else if (this.text != text) {
		fadOutAnimation(duration, View.INVISIBLE, fadeOutInterpolator) {
			this.text = text
			fadInAnimation(duration, interpolator = fadeInInterpolator, completion = {
				completion?.let {
					it()
				}
			})
		}
	} else {
		completion?.let { it() }
	}
}

fun SensorManager.checkSensorAvailability(sensorType: Int): Boolean {
	return getDefaultSensor(sensorType) != null
}

// the whole point of this function is to do literally nothing at all (but without impacting
// performance) in release builds and ignore StrictMode violations in debug builds
inline fun <reified T> allowDiskAccessInStrictMode(doIt: () -> T): T {
	return if (BuildConfig.DEBUG) {
		if (Looper.getMainLooper() != Looper.myLooper()) throw IllegalStateException()
		val policy = StrictMode.allowThreadDiskReads()
		try {
			StrictMode.allowThreadDiskWrites()
			doIt()
		} finally {
			StrictMode.setThreadPolicy(policy)
		}
	} else doIt()
}

fun View.enableEdgeToEdgePaddingListener(ime: Boolean = false, top: Boolean = false,
										 extra: ((Insets) -> Unit)? = null) {
	if (fitsSystemWindows) throw IllegalArgumentException("must have fitsSystemWindows disabled")
	if (this is AppBarLayout) {
		if (ime) throw IllegalArgumentException("AppBarLayout must have ime flag disabled")
		// AppBarLayout fitsSystemWindows does not handle left/right for a good reason, it has
		// to be applied to children to look good; we rewrite fitsSystemWindows in a way mostly specific
		// to Gramophone to support shortEdges displayCutout
		val collapsingToolbarLayout = children.find { it is CollapsingToolbarLayout } as CollapsingToolbarLayout?
		collapsingToolbarLayout?.let {
			// The CollapsingToolbarLayout mustn't consume insets, we handle padding here anyway
			ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets -> insets }
		}
		val expandedTitleMarginStart = collapsingToolbarLayout?.expandedTitleMarginStart
		val expandedTitleMarginEnd = collapsingToolbarLayout?.expandedTitleMarginEnd
		ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
			val cutoutAndBars = insets.getInsets(
				WindowInsetsCompat.Type.systemBars()
						or WindowInsetsCompat.Type.displayCutout()
			)
			(v as AppBarLayout).children.forEach {
				if (it is CollapsingToolbarLayout) {
					val es = expandedTitleMarginStart!! + if (it.layoutDirection
						== View.LAYOUT_DIRECTION_LTR) cutoutAndBars.left else cutoutAndBars.right
					if (es != it.expandedTitleMarginStart) it.expandedTitleMarginStart = es
					val ee = expandedTitleMarginEnd!! + if (it.layoutDirection
						== View.LAYOUT_DIRECTION_RTL) cutoutAndBars.left else cutoutAndBars.right
					if (ee != it.expandedTitleMarginEnd) it.expandedTitleMarginEnd = ee
				}
				it.setPadding(cutoutAndBars.left, 0, cutoutAndBars.right, 0)
			}
			v.setPadding(0, cutoutAndBars.top, 0, 0)
			val i = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()
					or WindowInsetsCompat.Type.displayCutout())
			extra?.invoke(cutoutAndBars)
			return@setOnApplyWindowInsetsListener WindowInsetsCompat.Builder(insets)
				.setInsets(WindowInsetsCompat.Type.systemBars()
						or WindowInsetsCompat.Type.displayCutout(), Insets.of(cutoutAndBars.left, 0, cutoutAndBars.right, cutoutAndBars.bottom))
				.setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars()
						or WindowInsetsCompat.Type.displayCutout(), Insets.of(i.left, 0, i.right, i.bottom))
				.build()
		}
	} else {
		val pl = paddingLeft
		val pt = paddingTop
		val pr = paddingRight
		val pb = paddingBottom
		ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
			val mask = WindowInsetsCompat.Type.systemBars() or
					WindowInsetsCompat.Type.displayCutout() or
					if (ime) WindowInsetsCompat.Type.ime() else 0
			val i = insets.getInsets(mask)
			v.setPadding(pl + i.left, pt + (if (top) i.top else 0), pr + i.right,
				pb + i.bottom)
			extra?.invoke(i)
			return@setOnApplyWindowInsetsListener WindowInsetsCompat.Builder(insets)
				.setInsets(mask, Insets.NONE)
				.setInsetsIgnoringVisibility(mask, Insets.NONE)
				.build()
		}
	}
}

@Suppress("NOTHING_TO_INLINE")
inline fun Int.dpToPx(context: Context): Int =
	(this.toFloat() * context.resources.displayMetrics.density).toInt()