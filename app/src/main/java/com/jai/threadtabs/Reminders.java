package com.jai.threadtabs;
import android.app.*;import android.content.*;import android.os.Build;import org.json.*;import java.util.*;

final class Reminders {
    static JSONArray all(Context c){try{return new JSONArray(c.getSharedPreferences("reminders",0).getString("items","[]"));}catch(Exception e){return new JSONArray();}}
    static void write(Context c,JSONArray a){c.getSharedPreferences("reminders",0).edit().putString("items",a.toString()).commit();}
    static String add(Context c,String mode,SmsData.Message m,long when)throws Exception{JSONObject r=new JSONObject().put("id",UUID.randomUUID().toString()).put("mode",mode).put("thread",m.thread).put("message",m.id).put("title",m.name).put("when",when).put("fired",false);JSONArray a=all(c);a.put(r);write(c,a);schedule(c,r);return r.getString("id");}
    static PendingIntent pending(Context c,String id){Intent i=new Intent(c,ReminderReceiver.class).setAction("com.jai.threadtabs.REMIND").setData(android.net.Uri.parse("threadtabs-reminder:"+id)).putExtra("id",id);return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static void schedule(Context c,JSONObject r){if(r.optBoolean("fired"))return;((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,Math.max(System.currentTimeMillis()+1000,r.optLong("when")),pending(c,r.optString("id")));}
    static void reschedule(Context c){JSONArray a=all(c);for(int i=0;i<a.length();i++)schedule(c,a.optJSONObject(i));}
    static void remove(Context c,String id){JSONArray a=all(c),out=new JSONArray();for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(!r.optString("id").equals(id))out.put(r);}write(c,out);((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c,id));((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id,1);}
    static void snooze(Context c,String id){JSONArray a=all(c);for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(r.optString("id").equals(id))try{r.put("when",System.currentTimeMillis()+3600000).put("fired",false);schedule(c,r);}catch(Exception ignored){}}write(c,a);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id,1);}
}
