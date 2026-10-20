package com.example.deltareflexautotapper

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/** Experimental blue-triangle heuristic. It must be validated against real game frames. */
object ArrowDetector {
 fun detect(b:Bitmap):String? {
  val w=b.width; val h=b.height
  val l=(w*.25f).toInt(); val r=(w*.75f).toInt(); val t=(h*.34f).toInt(); val bot=(h*.54f).toInt()
  var minX=r; var maxX=l; var minY=bot; var maxY=t; var count=0
  val step=max(1,min(w,h)/500)
  fun blue(x:Int,y:Int):Boolean { val c=b.getPixel(x,y); val rr=(c shr 16) and 255; val g=(c shr 8) and 255; val bl=c and 255; return bl>170 && g>65 && bl>rr*1.35 && bl>g*1.05 }
  for(y in t until bot step step) for(x in l until r step step) if(blue(x,y)) { minX=min(minX,x); maxX=max(maxX,x); minY=min(minY,y); maxY=max(maxY,y); count++ }
  if(count<25) return null
  val bw=maxX-minX+1; val bh=maxY-minY+1
  if(bw<w*.025 || bh<h*.012) return null
  fun edge(which:String):Int {
   var n=0; val bx=max(1,bw/7); val by=max(1,bh/7)
   for(y in minY..maxY step step) for(x in minX..maxX step step) {
    val band=when(which) { "L"->x<minX+bx; "R"->x>maxX-bx; "U"->y<minY+by; else->y>maxY-by }
    if(band && blue(x,y)) n++
   }; return n
  }
  val e=listOf("LEFT" to edge("L"),"RIGHT" to edge("R"),"UP" to edge("U"),"DOWN" to edge("D")).sortedBy{it.second}
  if(e[1].second < e[0].second*1.12) return null
  return e[0].first
 }
}
