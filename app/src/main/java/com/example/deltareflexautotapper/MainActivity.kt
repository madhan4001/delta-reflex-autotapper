package com.example.deltareflexautotapper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 private val captureRequest = 410
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(36,36,36,36) }
  root.addView(TextView(this).apply { text="Delta Reflex Auto-Tapper\n\n1. Grant overlay permission\n2. Enable accessibility service\n3. Start screen capture\n4. Open the game and test carefully.\n\nExperimental prototype: detection and coordinates require testing."; textSize=18f })
  root.addView(Button(this).apply { text="Grant overlay permission"; setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) } })
  root.addView(Button(this).apply { text="Enable tap accessibility"; setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } })
  root.addView(Button(this).apply { text="Start capture"; setOnClickListener { val m=getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager; startActivityForResult(m.createScreenCaptureIntent(),captureRequest) } })
  root.addView(Button(this).apply { text="Stop capture"; setOnClickListener { stopService(Intent(this@MainActivity,CaptureService::class.java)); stopService(Intent(this@MainActivity,FloatingControlsService::class.java)) } })
  setContentView(root)
 }
 @Deprecated("Legacy activity result API")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==captureRequest && resultCode==RESULT_OK && data!=null) {
   val i=Intent(this,CaptureService::class.java).putExtra("resultCode",resultCode).putExtra("projectionData",data)
   startForegroundService(i); startService(Intent(this,FloatingControlsService::class.java))
  }
 }
}
