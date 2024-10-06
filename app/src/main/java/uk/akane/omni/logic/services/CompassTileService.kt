package uk.akane.omni.logic.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.view.Surface
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import uk.akane.omni.R
import kotlin.math.absoluteValue

class CompassTileService : TileService(), SensorEventListener {

    private val sensorManager
        get() = getSystemService<SensorManager>()
    private val rotationVectorSensor
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val notificationManager
        get() = getSystemService<NotificationManager>()
    private lateinit var rotationIcon: Drawable
    private lateinit var iconBitmap: Bitmap


    companion object {
        const val CHANNEL_ID = "COMPASS_CHANNEL"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.compass_tile_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        rotationIcon = AppCompatResources.getDrawable(this, R.drawable.ic_pointer)!!
        iconBitmap = Bitmap.createBitmap(
            rotationIcon.intrinsicWidth, rotationIcon.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
    }

    override fun onClick() {
        super.onClick()
        Log.d("TAG", "onClick")
        qsTile.state = when (qsTile.state) {
            Tile.STATE_ACTIVE -> Tile.STATE_INACTIVE
            Tile.STATE_INACTIVE -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (qsTile.state == Tile.STATE_INACTIVE) {
            qsTile.label = getString(R.string.compass)
        }
        qsTile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d("TAG", "START LISTENING")
        if (qsTile.state == Tile.STATE_ACTIVE) {
            startForeground(
                NOTIFICATION_ID,
                NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_explorer)
                    .setContentTitle(getString(R.string.compass_notification_title))
                    .setContentText(getString(R.string.compass_notification_label))
                    .build()
            )
            sensorManager?.registerListener(
                this,
                rotationVectorSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        if (qsTile.state == Tile.STATE_ACTIVE) {
            sensorManager?.unregisterListener(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        Log.d("TAG", "STOP LISTENING")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR && qsTile.state == Tile.STATE_ACTIVE) {
            updateCompass(event)
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun updateCompass(event: SensorEvent) {
        val rotationVector = event.values.take(3).toFloatArray()
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        val displayRotation = ContextCompat.getDisplayOrDefault(baseContext).rotation
        val remappedRotationMatrix = remapRotationMatrix(rotationMatrix, displayRotation)

        val orientationInRadians = FloatArray(3)
        SensorManager.getOrientation(remappedRotationMatrix, orientationInRadians)

        val azimuthInDegrees = Math.toDegrees(orientationInRadians[0].toDouble()).toFloat()
        val adjustedAzimuth = (azimuthInDegrees + 360) % 360

        Canvas(iconBitmap).apply {
            drawColor(Color.BLACK, PorterDuff.Mode.CLEAR) // clear all
            rotate(-adjustedAzimuth, width / 2f, height / 2f)
            rotationIcon.setBounds(0, 0, width, height)
            rotationIcon.draw(this)
        }

        qsTile.label = getString(R.string.degree_format_tile, adjustedAzimuth.toInt().absoluteValue)
        qsTile.icon = Icon.createWithBitmap(iconBitmap)

        qsTile.updateTile()
    }

    private fun remapRotationMatrix(rotationMatrix: FloatArray, displayRotation: Int): FloatArray {
        val (newX, newY) = when (displayRotation) {
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y)
        }

        val remappedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(rotationMatrix, newX, newY, remappedRotationMatrix)
        return remappedRotationMatrix
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
