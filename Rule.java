package com.fruitninjabot;

public class Rule {
    public String id,name,action; public float x1,y1,x2,y2; public long duration,cooldown; public float threshold;
    public Rule(String id,String name,String action,float x1,float y1,float x2,float y2,long duration,long cooldown,float threshold){
        this.id=id;this.name=name;this.action=action;this.x1=x1;this.y1=y1;this.x2=x2;this.y2=y2;this.duration=duration;this.cooldown=cooldown;this.threshold=threshold;
    }
}
