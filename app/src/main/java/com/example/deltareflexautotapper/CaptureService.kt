package com.example.deltareflexautotapper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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

class CaptureService:Service() {
 private var projection:MediaProjection?=null; private var display:VirtualDisplay?=null; private var reader:ImageReader?=null
 private var thread:HandlerThread?=null; private var handler:Handler?=null; private var lastDirection:String?=null; private var lastTap=0L
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  notifyForeground()
  val code=intent?.getIntExtra("resultCode",0)?:0
  @Suppress("DEPRECATION") val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("projectionData",Intent::class.java) else intent?.getParcelableExtra("projectionData")
  if(data==null){stopSelf();return START_NOT_STICKY}
  val mgr=getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  projection=mgr.getMediaProjection(code,data)
  val dm=resources.displayMetrics; val w=dm.widthPixels; val h=dm.heightPixels
  thread=HandlerThread("ArrowCapture").also{it.start()}; handler=Handler(thread!!.looper)
  reader=ImageReader.newInstance(w,h,android.graphics.PixelFormat.RGBA_8888,2)
  projection?.registerCallback(object:MediaProjection.Callback(){override fun onStop(){stopSelf()}},handler)
  display=projection?.createVirtualDisplay("DeltaReflexCapture",w,h,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader!!.surface,null,handler)
  handler?.post(loop); return START_NOT_STICKY
 }
 private val loop=object:Runnable { override fun run() {
  var img:Image?=null
  try {
   img=reader?.acquireLatestImage()
   if(img!=null) {
    val p=img.planes[0]; val buf=p.buffer; val padded=img.width+(p.rowStride-p.pixelStride*img.width)/p.pixelStride
    val full=Bitmap.createBitmap(padded,img.height,Bitmap.Config.ARGB_8888); full.copyPixelsFromBuffer(buf)
    val bmp=Bitmap.createBitmap(full,0,0,img.width,img.height); full.recycle()
    val dir=ArrowDetector.detect(bmp); bmp.recycle(); val now=System.currentTimeMillis()
    if(dir!=null && dir!=lastDirection && now-lastTap>=350) {
     val point=when(dir){"UP"->.50f to .535f;"LEFT"->.365f to .590f;"DOWN"->.50f to .590f;"RIGHT"->.635f to .590f;else->null}
     if(point!=null) { TapAccessibilityService.instance?.tapNormalized(point.first,point.second); lastTap=now; lastDirection=dir }
    }
   }
  } catch(_:Exception) {} finally { img?.close(); handler?.postDelayed(this,125L) }
 } }
 private fun notifyForeground() {
  val id="delta_capture"
  if(Build.VERSION.SDK_INT>=26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(id,"Screen capture",NotificationManager.IMPORTANCE_LOW))
  val n=if(Build.VERSION.SDK_INT>=26) Notification.Builder(this,id).setContentTitle("Delta Reflex Auto-Tapper").setContentText("Screen capture active").setSmallIcon(android.R.drawable.ic_media_play).build()
   else @Suppress("DEPRECATION") (Notification.Builder(this).setContentTitle("Delta Reflex Auto-Tapper").setSmallIcon(android.R.drawable.ic_media_play).build())
  startForeground(1001,n)
 }
 override fun onDestroy(){handler?.removeCallbacksAndMessages(null);display?.release();reader?.close();projection?.stop();thread?.quitSafely();super.onDestroy()}
}
