package com.example.deltareflexautotapper

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout

class FloatingControlsService:Service() {
 private var wm:WindowManager?=null; private var panel:LinearLayout?=null
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onCreate() {
  super.onCreate(); if(!Settings.canDrawOverlays(this)){stopSelf();return}
  wm=getSystemService(WINDOW_SERVICE) as WindowManager
  val p=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(8,6,8,6);setBackgroundColor(0xEE172554.toInt())}
  p.addView(Button(this).apply{text="STOP AUTO-TAP";setOnClickListener{stopService(Intent(this@FloatingControlsService,CaptureService::class.java));text="STOPPED"}})
  p.addView(Button(this).apply{text="OPEN APP";setOnClickListener{val i=packageManager.getLaunchIntentForPackage(packageName);i?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(i!=null)startActivity(i)}})
  val type=if(Build.VERSION.SDK_INT>=26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
  val lp=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT).apply{gravity=Gravity.TOP or Gravity.END;x=20;y=160}
  wm?.addView(p,lp);panel=p
 }
 override fun onDestroy(){try{panel?.let{wm?.removeView(it)}}catch(_:Exception){};super.onDestroy()}
}
