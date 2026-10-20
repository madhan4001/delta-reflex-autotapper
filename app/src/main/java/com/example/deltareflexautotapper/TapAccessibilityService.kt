package com.example.deltareflexautotapper

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class TapAccessibilityService : AccessibilityService() {
 override fun onAccessibilityEvent(event:AccessibilityEvent?) {}
 override fun onInterrupt() {}
 fun tapNormalized(xf:Float,yf:Float) {
  val dm=resources.displayMetrics
  val x=(dm.widthPixels*xf).coerceIn(1f,dm.widthPixels-1f)
  val y=(dm.heightPixels*yf).coerceIn(1f,dm.heightPixels-1f)
  val path=Path().apply { moveTo(x,y) }
  val gesture=GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path,0,55)).build()
  dispatchGesture(gesture,null,null)
 }
 companion object { @Volatile var instance:TapAccessibilityService?=null }
 override fun onServiceConnected() { super.onServiceConnected(); instance=this }
 override fun onDestroy() { instance=null; super.onDestroy() }
}
