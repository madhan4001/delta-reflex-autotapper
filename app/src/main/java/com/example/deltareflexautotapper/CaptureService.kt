
package com.example.deltareflexautotapper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    private var lastDirection: String? = null
    private var lastTap = 0L
    private var missedFrames = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        notifyForeground()

        val code = intent?.getIntExtra("resultCode", 0) ?: 0

        @Suppress("DEPRECATION")
        val data: Intent? =
            if (Build.VERSION.SDK_INT >= 33) {
                intent?.getParcelableExtra("projectionData", Intent::class.java)
            } else {
                intent?.getParcelableExtra("projectionData")
            }

        if (data == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val manager = getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        projection = manager.getMediaProjection(code, data)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        thread = HandlerThread("ArrowCapture").also { it.start() }
        handler = Handler(thread!!.looper)

        reader = ImageReader.newInstance(
            width,
            height,
            PixelFormat.RGBA_8888,
            2
        )

        projection?.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    stopSelf()
                }
            },
            handler
        )

        display = projection?.createVirtualDisplay(
            "DeltaReflexCapture",
            width,
            height,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface,
            null,
            handler
        )

        handler?.post(loop)
        return START_NOT_STICKY
    }

    private val loop = object : Runnable {
        override fun run() {
            var image: Image? = null

            try {
                image = reader?.acquireLatestImage()

                if (image != null) {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val paddedWidth = image.width +
                        (plane.rowStride - plane.pixelStride * image.width) /
                        plane.pixelStride

                    val fullBitmap = Bitmap.createBitmap(
                        paddedWidth,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    )
                    fullBitmap.copyPixelsFromBuffer(buffer)

                    val bitmap = Bitmap.createBitmap(
                        fullBitmap,
                        0,
                        0,
                        image.width,
                        image.height
                    )
                    fullBitmap.recycle()

                    val direction = ArrowDetector.detect(bitmap)
                    bitmap.recycle()

                    val now = System.currentTimeMillis()

                    if (direction == null) {
                        missedFrames++

                        // Clear the previous direction after the arrow
                        // disappears for several frames.
                        if (missedFrames >= 4) {
                            lastDirection = null
                        }
                    } else {
                        missedFrames = 0

                        if (
                            direction != lastDirection &&
                            now - lastTap >= 350
                        ) {
                            val tapper = TapAccessibilityService.instance

                            val point = when (direction) {
                                "UP" -> 0.50f to 0.535f
                                "LEFT" -> 0.365f to 0.590f
                                "DOWN" -> 0.50f to 0.590f
                                "RIGHT" -> 0.635f to 0.590f
                                else -> null
                            }

                            // Only mark the direction as tapped when the
                            // accessibility tap service is available.
                            if (tapper != null && point != null) {
                                tapper.tapNormalized(
                                    point.first,
                                    point.second
                                )
                                lastTap = now
                                lastDirection = direction
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                android.util.Log.e(
                    "DeltaReflexCapture",
                    "Frame processing failed",
                    exception
                )
            } finally {
                image?.close()
                handler?.postDelayed(this, 125L)
            }
        }
    }

    private fun notifyForeground() {
        val channelId = "delta_capture"

        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Screen capture",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
                .setContentTitle("Delta Reflex Auto-Tapper")
                .setContentText("Screen capture active")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Delta Reflex Auto-Tapper")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        display?.release()
        reader?.close()
        projection?.stop()
        thread?.quitSafely()
        super.onDestroy()
    }
}
