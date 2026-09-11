package com.jai.threadtabs;

import android.app.IntentService;
import android.content.Intent;

/** Supports Android's quick "respond via message" contract for the default SMS role. */
public class RespondViaMessageService extends IntentService {
    public RespondViaMessageService(){super("ThreadTabsRespondViaMessage");}

    @Override protected void onHandleIntent(Intent intent){
        if(intent==null||intent.getData()==null)return;
        String address=intent.getData().getSchemeSpecificPart();CharSequence raw=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);String body=raw==null?"":raw.toString();
        if(address==null||address.trim().isEmpty()||body.trim().isEmpty())return;
        SmsSender.send(this,address,body,(ok,message)->{});
    }
}
