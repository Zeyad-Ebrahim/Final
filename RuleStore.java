package com.fruitninjabot;

import android.content.Context;
import java.util.*;

public class RuleStore {
    private static final String PREF="rules";
    public static ArrayList<Rule> load(Context c){
        String raw=c.getSharedPreferences(PREF,0).getString("data",""); ArrayList<Rule> out=new ArrayList<>();
        if(raw.isEmpty()) return out;
        for(String s:raw.split("\\n",-1)){ try{String[] a=s.split("\\|",-1); if(a.length<10)continue;
            out.add(new Rule(a[0],a[1],a[2],Float.parseFloat(a[3]),Float.parseFloat(a[4]),Float.parseFloat(a[5]),Float.parseFloat(a[6]),Long.parseLong(a[7]),Long.parseLong(a[8]),Float.parseFloat(a[9])));
        }catch(Exception ignored){}}
        return out;
    }
    public static void save(Context c,ArrayList<Rule> rules){
        StringBuilder b=new StringBuilder(); for(Rule r:rules){b.append(clean(r.id)).append('|').append(clean(r.name)).append('|').append(clean(r.action)).append('|')
            .append(r.x1).append('|').append(r.y1).append('|').append(r.x2).append('|').append(r.y2).append('|').append(r.duration).append('|').append(r.cooldown).append('|').append(r.threshold).append('\n');}
        c.getSharedPreferences(PREF,0).edit().putString("data",b.toString()).apply();
    }
    private static String clean(String s){return s.replace("|","_").replace("\n","_");}
}
