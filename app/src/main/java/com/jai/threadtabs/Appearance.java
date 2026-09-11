package com.jai.threadtabs;

import android.content.*;
import android.graphics.*;
import org.json.*;

final class Appearance {
    static final String[] FONTS={"sans-serif","serif","monospace","cursive","sans-serif-condensed","sans-serif-light"};
    static JSONObject get(Context c,String mode,String id){try{return new JSONObject(c.getSharedPreferences("appearance_"+mode,0).getString(id,"{}"));}catch(Exception e){return new JSONObject();}}
    static void save(Context c,String mode,String id,JSONObject style){c.getSharedPreferences("appearance_"+mode,0).edit().putString(id,style.toString()).apply();}
    static int color(JSONObject s,String key,int fallback){try{return Color.parseColor(s.getString(key));}catch(Exception e){return fallback;}}
    static Typeface font(Context c,JSONObject s){
        String global=c.getSharedPreferences("settings",0).getString("font","sans-serif");
        String path=s.optString("fontPath","");
        try {if(!path.isEmpty())return Typeface.create(Typeface.createFromFile(path),s.optInt("weight",0));}catch(Exception ignored){}
        return Typeface.create(s.optString("font",global),s.optInt("weight",0));
    }
}
