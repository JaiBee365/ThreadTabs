package com.jai.threadtabs;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/** Receives SMS_DELIVER broadcasts when Haven Message is the default SMS app. */
public class SmsDeliverReceiver extends BroadcastReceiver {
    private static final String CHANNEL="incoming-sms";

    @Override public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;
        if (Build.VERSION.SDK_INT >= 19 && !context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context))) return;
        SmsMessage[] parts=Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts==null||parts.length==0) return;
        String address=parts[0].getDisplayOriginatingAddress();StringBuilder body=new StringBuilder();long date=parts[0].getTimestampMillis();
        for(SmsMessage part:parts){if(part!=null){body.append(part.getMessageBody());date=Math.max(date,part.getTimestampMillis());}}
        try{
            ContentValues values=new ContentValues();values.put(Telephony.TextBasedSmsColumns.ADDRESS,address);values.put(Telephony.TextBasedSmsColumns.BODY,body.toString());values.put(Telephony.TextBasedSmsColumns.DATE,date);values.put(Telephony.TextBasedSmsColumns.READ,0);values.put(Telephony.TextBasedSmsColumns.SEEN,0);values.put(Telephony.TextBasedSmsColumns.TYPE,Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI,values);
        }catch(Exception ignored){}
        notifyUser(context,address,body.toString());setResultCode(Activity.RESULT_OK);
    }

    private void notifyUser(Context context,String address,String body){
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(manager==null)return;
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Incoming texts",NotificationManager.IMPORTANCE_HIGH));
        Intent open=new Intent(context,MainActivity.class).putExtra("open_mode","live").setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending=PendingIntent.getActivity(context,(int)(System.currentTimeMillis()&0x7fffffff),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);
        Notification notification=builder.setSmallIcon(R.drawable.ic_notice).setContentTitle(address==null?"New text":address).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(pending).setAutoCancel(true).setCategory(Notification.CATEGORY_MESSAGE).setVisibility(Notification.VISIBILITY_PRIVATE).build();
        manager.notify((int)(System.currentTimeMillis()&0x7fffffff),notification);
    }
}
