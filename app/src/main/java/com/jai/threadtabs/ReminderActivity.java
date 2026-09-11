package com.jai.threadtabs;
import android.app.*;import android.os.*;import android.widget.*;import org.json.*;import java.text.DateFormat;import java.util.*;
public class ReminderActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);show();}
    private void show(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(28,64,28,28);ScrollView s=new ScrollView(this);s.addView(l);setContentView(s);TextView title=new TextView(this);title.setText("Reply reminders");title.setTextSize(26);l.addView(title);TextView note=new TextView(this);note.setText("Tap a message in a conversation to add a reminder. Android may deliver alerts later during power-saving. Reminders stay here until you mark them done.");l.addView(note);JSONArray a=Reminders.all(this);
        for(int i=0;i<a.length();i++){JSONObject r=a.optJSONObject(i);Button b=new Button(this);b.setAllCaps(false);b.setText(r.optString("title")+"\n"+DateFormat.getDateTimeInstance().format(new Date(r.optLong("when")))+(r.optBoolean("fired")?" · Notified":" · Pending"));b.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(r.optString("title")).setItems(new String[]{"Open conversation","Snooze 1 hour","Mark done / cancel"},(d,k)->{if(k==0)startActivity(new android.content.Intent(this,MainActivity.class).putExtra("open_mode",r.optString("mode")).putExtra("open_thread",r.optString("thread")));if(k==1)Reminders.snooze(this,r.optString("id"));if(k==2)Reminders.remove(this,r.optString("id"));show();}).show());l.addView(b);}
        Button close=new Button(this);close.setText("Close");close.setOnClickListener(v->finish());l.addView(close);
    }
}
