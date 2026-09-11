package com.jai.threadtabs;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Sends SMS from Haven Message and records successful sends in the system SMS provider. */
final class SmsSender {
    interface Callback { void done(boolean success, String message); }

    private SmsSender() {}

    static void send(Context context, String address, String body, Callback callback) {
        if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            callback.done(false, "SMS sending permission is not enabled.");
            return;
        }
        final String action = context.getPackageName()+".SMS_SENT_"+System.nanoTime();
        final SmsManager manager = SmsManager.getDefault();
        final ArrayList<String> parts = manager.divideMessage(body);
        final AtomicInteger remaining = new AtomicInteger(parts.size());
        final AtomicBoolean finished = new AtomicBoolean(false);
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                boolean ok = getResultCode() == android.app.Activity.RESULT_OK;
                if (!ok && finished.compareAndSet(false,true)) {
                    unregister(c, this);
                    callback.done(false, "The carrier could not send this text. Check signal, the number, and your messaging plan.");
                    return;
                }
                if (ok && remaining.decrementAndGet() == 0 && finished.compareAndSet(false,true)) {
                    unregister(c, this);
                    recordSent(c, address, body);
                    callback.done(true, "Text sent");
                }
            }
        };
        IntentFilter filter = new IntentFilter(action);
        try {
            if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else context.registerReceiver(receiver, filter);
            ArrayList<PendingIntent> sent = new ArrayList<>();
            for (int i=0;i<parts.size();i++) {
                Intent intent = new Intent(action).setPackage(context.getPackageName()).putExtra("part",i);
                sent.add(PendingIntent.getBroadcast(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
            }
            if (parts.size()==1) manager.sendTextMessage(address, null, body, sent.get(0), null);
            else manager.sendMultipartTextMessage(address, null, parts, sent, null);
        } catch (Exception e) {
            unregister(context, receiver);
            callback.done(false, e instanceof SecurityException ? "Android blocked SMS sending. Make Haven Message the default SMS app and allow SMS permissions." : "Could not start SMS sending.");
        }
    }

    private static void unregister(Context c, BroadcastReceiver r) { try { c.unregisterReceiver(r); } catch (Exception ignored) {} }

    private static void recordSent(Context c, String address, String body) {
        try {
            ContentValues values = new ContentValues();
            values.put(Telephony.TextBasedSmsColumns.ADDRESS, address);
            values.put(Telephony.TextBasedSmsColumns.BODY, body);
            values.put(Telephony.TextBasedSmsColumns.DATE, System.currentTimeMillis());
            values.put(Telephony.TextBasedSmsColumns.READ, 1);
            values.put(Telephony.TextBasedSmsColumns.SEEN, 1);
            values.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT);
            c.getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
        } catch (Exception ignored) {
            // The carrier send succeeded even if an unusual provider declines the local history insert.
        }
    }
}
