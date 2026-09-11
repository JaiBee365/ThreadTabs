package com.jai.threadtabs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;

/** Keeps the default-SMS role contract visible while MMS parsing remains outside this build. */
public class MmsDeliverReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if(Build.VERSION.SDK_INT>=19&&!context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context)))return;
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        String channel="incoming-mms";if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(channel,"MMS alerts",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(context,7,new Intent(context,MainActivity.class).putExtra("open_mode","live"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,channel):new Notification.Builder(context);
        manager.notify(7007,builder.setSmallIcon(R.drawable.ic_notice).setContentTitle("MMS received").setContentText("MMS media is not loaded by this Haven Message build.").setContentIntent(open).setAutoCancel(true).build());
    }
}
