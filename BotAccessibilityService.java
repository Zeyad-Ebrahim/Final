package com.fruitninjabot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class BotAccessibilityService extends AccessibilityService {
    private static volatile BotAccessibilityService instance;
    public static BotAccessibilityService getInstance(){ return instance; }
    @Override public void onServiceConnected(){ super.onServiceConnected(); instance=this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){ instance=null; }
    @Override public void onDestroy(){ instance=null; super.onDestroy(); }

    public boolean tap(float x,float y){ return gesture(new float[]{x,y}, new float[]{x,y}, 70); }
    public boolean swipe(float x1,float y1,float x2,float y2,long duration){ return gesture(new float[]{x1,y1},new float[]{x2,y2},Math.max(40,duration)); }
    private boolean gesture(float[] a,float[] b,long duration){
        Path p=new Path(); p.moveTo(a[0],a[1]); p.lineTo(b[0],b[1]);
        GestureDescription.StrokeDescription s=new GestureDescription.StrokeDescription(p,0,duration);
        return dispatchGesture(new GestureDescription.Builder().addStroke(s).build(),null,null);
    }
}
