package com.fruitninjabot;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.DisplayManager;
import android.media.*;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.view.WindowManager;
import java.nio.ByteBuffer;
import java.util.*;

public class CaptureService extends Service {
    private MediaProjection projection; private ImageReader reader; private Handler handler; private int W,H; private long lastGesture=0; private boolean running=true;
    private final ArrayList<Rule> rules=new ArrayList<>(); private final HashMap<String,Bitmap> templateCache=new HashMap<>(); private final HashMap<String,Long> ruleLast=new HashMap<>();
    private static final int SCALE=4;
    @Override public void onCreate(){super.onCreate();handler=new Handler(Looper.getMainLooper());createChannel();startForeground(42,notification());rules.addAll(RuleStore.load(this));}
    private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel("bot","Fruit Ninja Bot",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(c);}}
    private Notification notification(){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"bot"):new Notification.Builder(this);return b.setContentTitle("Fruit Ninja Bot").setContentText("Bot is running").setSmallIcon(android.R.drawable.ic_media_play).build();}
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i==null)return START_STICKY;
        rules.clear();rules.addAll(RuleStore.load(this));
        int code=i.getIntExtra("resultCode",0); Intent data=i.getParcelableExtra("data");
        if(data!=null && projection==null){
            MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);projection=m.getMediaProjection(code,data);
            WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE); android.util.DisplayMetrics dm=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(dm);W=dm.widthPixels;H=dm.heightPixels;
            reader=ImageReader.newInstance(W,H,PixelFormat.RGBA_8888,2);reader.setOnImageAvailableListener(r->process(),handler);
            projection.createVirtualDisplay("FruitNinjaBot",W,H,dm.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
        }
        return START_STICKY;
    }
    private void process(){if(!running||reader==null)return;Image im=null;try{im=reader.acquireLatestImage();if(im==null)return;Image.Plane p=im.getPlanes()[0];ByteBuffer buf=p.getBuffer();int ps=p.getPixelStride(),rs=p.getRowStride();int rowW=rs/ps;Bitmap raw=Bitmap.createBitmap(rowW,H,Bitmap.Config.ARGB_8888);buf.rewind();raw.copyPixelsFromBuffer(buf);Bitmap full=Bitmap.createBitmap(raw,0,0,W,H);raw.recycle();Bitmap small=Bitmap.createScaledBitmap(full,Math.max(1,W/SCALE),Math.max(1,H/SCALE),false);full.recycle();
            if(handleRules(small)){small.recycle();return;} analyze(small);small.recycle();
        }catch(Exception ignored){}finally{if(im!=null)im.close();}}

    private boolean handleRules(Bitmap screen){
        long now=System.currentTimeMillis();
        for(Rule r:rules){
            Bitmap t=templateCache.get(r.id);if(t==null){t=TemplateStore.load(this,r.id);if(t!=null){Bitmap s=Bitmap.createScaledBitmap(t,24,16,false);if(t!=s)t.recycle();t=s;templateCache.put(r.id,t);}}
            if(t==null)continue; long last=ruleLast.containsKey(r.id)?ruleLast.get(r.id):0L;if(now-last<r.cooldown)continue;
            float sim=similarity(screen,t);if(sim>=r.threshold){float x1=r.x1*W,y1=r.y1*H,x2=r.x2*W,y2=r.y2*H;BotAccessibilityService s=BotAccessibilityService.getInstance();if(s!=null){if("TAP".equals(r.action))s.tap(x1,y1);else s.swipe(x1,y1,x2,y2,r.duration);ruleLast.put(r.id,now);return true;}}
        }return false;
    }
    private float similarity(Bitmap a,Bitmap b){Bitmap x=Bitmap.createScaledBitmap(a,24,16,false);long diff=0;for(int y=0;y<16;y++)for(int xx=0;xx<24;xx++){int ca=x.getPixel(xx,y),cb=b.getPixel(xx,y);diff+=Math.abs(Color.red(ca)-Color.red(cb))+Math.abs(Color.green(ca)-Color.green(cb))+Math.abs(Color.blue(ca)-Color.blue(cb));}x.recycle();return 1f-(diff/(float)(24*16*765));}

    static class Obj{float x,y,r;Obj(float x,float y,float r){this.x=x;this.y=y;this.r=r;}}
    private void analyze(Bitmap b){int w=b.getWidth(),h=b.getHeight();boolean[][] fruit=new boolean[h][w],bomb=new boolean[h][w];for(int y=18;y<h-4;y++)for(int x=2;x<w-2;x++){int c=b.getPixel(x,y);float[] hsv=new float[3];Color.colorToHSV(c,hsv);float hu=hsv[0],sat=hsv[1],val=hsv[2];if(sat>.42f&&val>.40f){boolean purp=(hu>=250&&hu<=335&&sat>.48f&&val>.28f);if(purp)bomb[y][x]=true;else if((hu<55)||(hu>315)||(hu>=65&&hu<190)||(hu>=190&&hu<250))fruit[y][x]=true;}}
        ArrayList<Obj> bombs=components(b,bomb),fruits=components(b,fruit);fruits.removeIf(o->o.r<4||o.r>65||o.y<48||o.y>h-8);bombs.removeIf(o->o.r<5||o.r>58||o.y<48||o.y>h-8);fruits=merge(fruits,7);bombs=merge(bombs,8);fruits.sort((a,c)->Float.compare(c.y,a.y));ArrayList<Obj> targets=new ArrayList<>();for(Obj f:fruits){if(targets.size()>=5)break;boolean unsafe=false;for(Obj z:bombs)if(dist(f,z)<f.r+z.r+10){unsafe=true;break;}if(!unsafe)targets.add(f);}if(targets.isEmpty())return;long now=System.currentTimeMillis();if(now-lastGesture<90)return;
        ArrayList<PointF> path=new ArrayList<>();Obj prev=null;for(Obj f:targets){if(prev==null){path.add(new PointF(f.x*SCALE,f.y*SCALE));prev=f;}else if(safeSegment(prev,f,bombs)){path.add(new PointF(f.x*SCALE,f.y*SCALE));prev=f;}}if(path.isEmpty())return;if(path.size()==1){PointF q=path.get(0);path.add(new PointF(q.x+38,q.y-42));}float speed= getSharedPreferences("settings",0).getInt("speed",90);long duration=(long)(speed+path.size()*22);Gesture(path,duration);lastGesture=now;}
    private ArrayList<Obj> components(Bitmap b,boolean[][] mask){int h=mask.length,w=mask[0].length;boolean[][] seen=new boolean[h][w];ArrayList<Obj> out=new ArrayList<>();int[] dx={1,-1,0,0,1,1,-1,-1},dy={0,0,1,-1,1,-1,1,-1};for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(mask[y][x]&&!seen[y][x]){ArrayDeque<Integer>q=new ArrayDeque<>();q.add(y*w+x);seen[y][x]=true;int n=0;float sx=0,sy=0;int minx=x,maxx=x,miny=y,maxy=y;while(!q.isEmpty()){int v=q.remove(),yy=v/w,xx=v%w;n++;sx+=xx;sy+=yy;minx=Math.min(minx,xx);maxx=Math.max(maxx,xx);miny=Math.min(miny,yy);maxy=Math.max(maxy,yy);for(int k=0;k<8;k++){int nx=xx+dx[k],ny=yy+dy[k];if(nx>=0&&nx<w&&ny>=0&&ny<h&&mask[ny][nx]&&!seen[ny][nx]){seen[ny][nx]=true;q.add(ny*w+nx);}}}if(n>=16){float rr=Math.max(maxx-minx,maxy-miny)/2f;out.add(new Obj(sx/n,sy/n,rr));}}return out;}
    private ArrayList<Obj> merge(ArrayList<Obj>a,float d){ArrayList<Obj>r=new ArrayList<>();for(Obj o:a){boolean done=false;for(Obj q:r)if(dist(o,q)<Math.max(d,(o.r+q.r)*.35f)){q.x=(q.x+o.x)/2;q.y=(q.y+o.y)/2;q.r=Math.max(q.r,o.r);done=true;break;}if(!done)r.add(o);}return r;}
    private float dist(Obj a,Obj b){return(float)Math.hypot(a.x-b.x,a.y-b.y);}private boolean safeSegment(Obj a,Obj b,ArrayList<Obj>bombs){float margin=getSharedPreferences("settings",0).getInt("safety",10)/SCALE;for(Obj z:bombs)if(pointSeg(z.x,z.y,a.x,a.y,b.x,b.y)<z.r+margin)return false;return true;}private float pointSeg(float px,float py,float ax,float ay,float bx,float by){float dx=bx-ax,dy=by-ay;if(dx==0&&dy==0)return(float)Math.hypot(px-ax,py-ay);float t=((px-ax)*dx+(py-ay)*dy)/(dx*dx+dy*dy);t=Math.max(0,Math.min(1,t));return(float)Math.hypot(px-(ax+t*dx),py-(ay+t*dy));}
    private void Gesture(ArrayList<PointF>pts,long duration){BotAccessibilityService s=BotAccessibilityService.getInstance();if(s==null)return;Path p=new Path();p.moveTo(pts.get(0).x,pts.get(0).y);for(int i=1;i<pts.size();i++)p.lineTo(pts.get(i).x,pts.get(i).y);GestureDescription.StrokeDescription st=new GestureDescription.StrokeDescription(p,0,Math.max(40,duration));s.dispatchGesture(new GestureDescription.Builder().addStroke(st).build(),null,null);}
    @Override public void onDestroy(){running=false;for(Bitmap b:templateCache.values())b.recycle();templateCache.clear();if(reader!=null)reader.close();if(projection!=null)projection.stop();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
