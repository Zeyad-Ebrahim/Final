package com.fruitninjabot;

import android.app.*;import android.content.*;import android.graphics.*;import android.graphics.drawable.*;import android.media.projection.MediaProjectionManager;import android.net.Uri;import android.os.*;import android.provider.Settings;import android.view.*;import android.widget.*;import java.io.*;import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_CAPTURE=7001,REQ_IMAGE=7002; LinearLayout root; TextView status; ArrayList<Rule> rules; EditText speed,safety;
    @Override public void onCreate(Bundle b){super.onCreate(b);rules=RuleStore.load(this);buildHome();}
    TextView tv(String s,int z){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setPadding(8,8,8,8);return t;}
    Button btn(String s){Button b=new Button(this);b.setText(s);return b;}
    void buildHome(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(28,20,28,20);ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
        TextView title=tv("🍉 Fruit Ninja Bot 2.0",28);title.setGravity(Gravity.CENTER);root.addView(title);
        status=tv("اضبط الإعدادات ثم أضف قواعد الصفحات.",15);root.addView(status);
        root.addView(tv("سرعة الـSwipe (ms): أقل = أسرع",15));speed=new EditText(this);speed.setInputType(2);speed.setText(String.valueOf(getSharedPreferences("settings",0).getInt("speed",90)));root.addView(speed);
        root.addView(tv("مسافة أمان القنبلة (px):",15));safety=new EditText(this);safety.setInputType(2);safety.setText(String.valueOf(getSharedPreferences("settings",0).getInt("safety",18)));root.addView(safety);
        Button save=btn("حفظ الإعدادات");save.setOnClickListener(v->saveSettings());root.addView(save);
        Button acc=btn("1) تفعيل Accessibility");acc.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));root.addView(acc);
        Button add=btn("+ إضافة قاعدة صفحة");add.setOnClickListener(v->pickTemplate());root.addView(add);
        root.addView(tv("قواعد الصفحات: اضغط الزر المحدد تلقائيًا عندما تتطابق الصورة.",14));renderRules();
        Button start=btn("▶ START BOT");start.setOnClickListener(v->requestCapture());root.addView(start);
        Button stop=btn("■ STOP BOT");stop.setOnClickListener(v->stopService(new Intent(this,CaptureService.class)));root.addView(stop);
        root.addView(tv("طريقة الاستخدام: خذ Screenshot للصفحة المطلوبة من اللعبة، ثم أضفه كقاعدة وحدد زر TAP أو خط SWIPE على الصورة. القواعد تفحص قبل تقطيع الفواكه، وبذلك يمكنها بدء الجيم التالي والتعامل مع شاشة المكافأة/الإعلان.",14));
    }
    void saveSettings(){try{int sp=Math.max(40,Integer.parseInt(speed.getText().toString()));int sa=Math.max(0,Integer.parseInt(safety.getText().toString()));getSharedPreferences("settings",0).edit().putInt("speed",sp).putInt("safety",sa).apply();status.setText("تم حفظ الإعدادات.");}catch(Exception e){status.setText("اكتب أرقام صحيحة.");}}
    void renderRules(){for(Rule r:rules){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);TextView t=tv(r.name+" — "+r.action+" — threshold "+r.threshold,14);row.addView(t,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));Button del=btn("حذف");del.setOnClickListener(v->{TemplateStore.delete(this,r.id);rules.remove(r);RuleStore.save(this,rules);buildHome();});row.addView(del);root.addView(row);}}
    void pickTemplate(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_IMAGE);}
    void requestCapture(){saveSettings();if(BotAccessibilityService.getInstance()==null){status.setText("فعّل Accessibility أولاً.");return;}MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),REQ_CAPTURE);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==REQ_CAPTURE&&c==RESULT_OK&&d!=null){Intent s=new Intent(this,CaptureService.class);s.putExtra("resultCode",c);s.putExtra("data",d);if(Build.VERSION.SDK_INT>=29)s.putExtra("android:foregroundServiceType",android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);status.setText("BOT شغال — افتح Fruit Ninja.");}else if(r==REQ_IMAGE&&c==RESULT_OK&&d!=null){try{Bitmap b=BitmapFactory.decodeStream(getContentResolver().openInputStream(d));if(b!=null)showRuleEditor(b);}catch(Exception e){status.setText("تعذر فتح الصورة.");}}}
    void showRuleEditor(Bitmap bitmap){
        final Dialog dialog=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(20,10,20,10);
        EditText name=new EditText(this);name.setHint("اسم الصفحة (مثال: Game Over)");box.addView(name);
        Spinner action=new Spinner(this);String[] opts={"TAP","SWIPE"};action.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,opts));box.addView(action);
        TemplateView image=new TemplateView(this,bitmap);box.addView(image,new LinearLayout.LayoutParams(-1,0,1));
        EditText duration=new EditText(this);duration.setInputType(2);duration.setHint("مدة SWIPE بالمللي ثانية (مثال 120)");duration.setText("120");box.addView(duration);
        EditText cooldown=new EditText(this);cooldown.setInputType(2);cooldown.setHint("Cooldown ms (مثال 1500)");cooldown.setText("1500");box.addView(cooldown);
        EditText threshold=new EditText(this);threshold.setInputType(8194);threshold.setHint("تطابق الصورة 0.70 - 1.00");threshold.setText("0.86");box.addView(threshold);
        TextView help=tv("المس الصورة: TAP = نقطة واحدة. SWIPE = أول لمسة بداية السحب، وثاني لمسة نهايته.",13);box.addView(help);
        LinearLayout buttons=new LinearLayout(this);Button cancel=btn("إلغاء"),save=btn("حفظ");buttons.addView(cancel,new LinearLayout.LayoutParams(0,60,1));buttons.addView(save,new LinearLayout.LayoutParams(0,60,1));box.addView(buttons);cancel.setOnClickListener(v->dialog.dismiss());
        save.setOnClickListener(v->{try{if(image.x1<0){help.setText("حدد نقطة الزر على الصورة أولاً.");return;}String id=UUID.randomUUID().toString();String nm=name.getText().toString().trim();if(nm.isEmpty())nm="Rule "+(rules.size()+1);String ac=(String)action.getSelectedItem();if("SWIPE".equals(ac)&&image.x2<0){help.setText("حدد نقطتين للـSWIPE.");return;}long dur=Math.max(40,Long.parseLong(duration.getText().toString()));long cd=Math.max(200,Long.parseLong(cooldown.getText().toString()));float th=Math.max(.50f,Math.min(1f,Float.parseFloat(threshold.getText().toString())));TemplateStore.save(this,id,bitmap);rules.add(new Rule(id,nm,ac,image.x1,image.y1,image.x2<0?image.x1:image.x2,image.y2<0?image.y1:image.y2,dur,cd,th));RuleStore.save(this,rules);dialog.dismiss();buildHome();}catch(Exception e){help.setText("تأكد من القيم المدخلة.");}});
        dialog.setContentView(box);Window w=dialog.getWindow();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.95),(int)(getResources().getDisplayMetrics().heightPixels*.90));dialog.show();if(w!=null)w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.95),(int)(getResources().getDisplayMetrics().heightPixels*.90));
    }
    static class TemplateView extends View {Bitmap b;Paint p=new Paint(3);float scale,ox,oy,x1=-1,y1=-1,x2=-1,y2=-1;boolean first=true;TemplateView(Context c,Bitmap b){super(c);this.b=b;setBackgroundColor(Color.BLACK);}
        protected void onDraw(Canvas c){super.onDraw(c);if(b==null)return;scale=Math.min(getWidth()/(float)b.getWidth(),getHeight()/(float)b.getHeight());ox=(getWidth()-b.getWidth()*scale)/2;oy=(getHeight()-b.getHeight()*scale)/2;c.drawBitmap(b,null,new RectF(ox,oy,ox+b.getWidth()*scale,oy+b.getHeight()*scale),p);p.setColor(Color.RED);p.setStrokeWidth(6);if(x1>=0)c.drawCircle(ox+x1*b.getWidth()*scale,oy+y1*b.getHeight()*scale,10,p);if(x2>=0)c.drawCircle(ox+x2*b.getWidth()*scale,oy+y2*b.getHeight()*scale,10,p);if(x1>=0&&x2>=0)c.drawLine(ox+x1*b.getWidth()*scale,oy+y1*b.getHeight()*scale,ox+x2*b.getWidth()*scale,oy+y2*b.getHeight()*scale,p);}
        public boolean onTouchEvent(android.view.MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float xx=Math.max(0,Math.min(b.getWidth(),(e.getX()-ox)/scale)),yy=Math.max(0,Math.min(b.getHeight(),(e.getY()-oy)/scale));float nx=xx/b.getWidth(),ny=yy/b.getHeight();if(first){x1=nx;y1=ny;first=false;}else{x2=nx;y2=ny;}invalidate();return true;}
    }
}
