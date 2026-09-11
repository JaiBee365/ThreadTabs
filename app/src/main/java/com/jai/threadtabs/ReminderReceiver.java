package com.jai.threadtabs;
import android.app.*;import android.content.*;import org.json.*;
public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        if(!"com.jai.threadtabs.REMIND".equals(intent.getAction())){Reminders.reschedule(c);return;}
        String id=intent.getStringExtra("id");if(id==null)return;JSONArray a=Reminders.all(c);
        for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);if(!id.equals(r.optString("id"))||r.optBoolean("fired"))continue;
            NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel("reply-reminders","Reply reminders",NotificationManager.IMPORTANCE_DEFAULT);ch.setDescription("Your saved follow-up reminders");nm.createNotificationChannel(ch);
            if(!nm.areNotificationsEnabled())return;
            Intent open=new Intent(c,MainActivity.class).setAction("reminder-"+id).putExtra("open_mode",r.optString("mode")).putExtra("open_thread",r.optString("thread")).putExtra("reminder_id",id);
            PendingIntent pi=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification n=new Notification.Builder(c,"reply-reminders").setSmallIcon(R.drawable.ic_notice).setContentTitle("Time to reply").setContentText("You saved a follow-up reminder. Tap to open the conversation.").setContentIntent(pi).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
            try{nm.notify(id,1,n);r.put("fired",true);Reminders.write(c,a);}catch(Exception ignored){}return;
        }
    }
}
