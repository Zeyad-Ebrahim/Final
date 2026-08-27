package com.fruitninjabot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;

public class TemplateStore {
    public static File file(Context c,String id){ return new File(c.getFilesDir(),"template_"+id+".png"); }
    public static void save(Context c,String id,Bitmap b) throws IOException { try(FileOutputStream o=new FileOutputStream(file(c,id))){b.compress(Bitmap.CompressFormat.PNG,100,o);} }
    public static Bitmap load(Context c,String id){ File f=file(c,id); return f.exists()?BitmapFactory.decodeFile(f.getAbsolutePath()):null; }
    public static void delete(Context c,String id){ file(c,id).delete(); }
}
