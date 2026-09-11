package com.jai.threadtabs;

import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Telephony;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private Store store;
    private android.content.SharedPreferences prefs;
    private String mode,tab="all",thread=null,query="",undo=null;
    private List<SmsData.Message> messages=new ArrayList<>();
    private final Set<String> selected=new HashSet<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private ListView list;
    private LinearLayout root,tabs,actions;
    private TextView subtitle,status;
    private boolean dark,loading=false,observing=false;
    private int bg,card,ink,muted,accent,loadVersion=0;
    private float textSize;
    private String error="",playingMessage="";
    private String pendingSendAddress="",pendingSendBody="";
    private static final int REQUEST_DEFAULT_SMS=201;
    private static final int REQUEST_SMS_PERMISSIONS=202;
    private ContentObserver observer;
    private final Runnable refresh=()->load();

    @Override public void onCreate(Bundle saved) {
        prefs=getSharedPreferences("settings",MODE_PRIVATE);
        dark=prefs.getBoolean("dark",false);
        setTheme(dark?R.style.AppThemeDark:R.style.AppTheme);
        super.onCreate(saved);
        mode=prefs.getString("mode","demo");
        if(saved!=null){tab=saved.getString("tab","all");thread=saved.getString("thread");query=saved.getString("query","");}
        store=new Store(this,mode);seedDemo();
        observer=new ContentObserver(main){@Override public void onChange(boolean self){main.removeCallbacks(refresh);main.postDelayed(refresh,500);}};
        openTarget(getIntent());build();
        handleMessagingIntent(getIntent());
    }
    private void openTarget(Intent intent){
        String requested=intent.getStringExtra("open_mode");
        if(requested!=null&&Arrays.asList("demo","live","import","email").contains(requested)){
            mode=requested;prefs.edit().putString("mode",mode).apply();store=new Store(this,mode);seedDemo();
            String target=intent.getStringExtra("open_thread");thread=target==null||target.isEmpty()?null:target;
            tab=intent.getStringExtra("open_group");if(tab==null||(!tab.equals("all")&&!store.ids().contains(tab)))tab="all";
            selected.clear();query="";undo=null;
        }
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);openTarget(intent);build();handleMessagingIntent(intent);load();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("tab",tab);out.putString("thread",thread);out.putString("query",query);}
    @Override protected void onResume(){super.onResume();Reminders.reschedule(this);load();}
    @Override protected void onPause(){super.onPause();playingMessage="";if(list!=null)list.setAdapter(null);main.removeCallbacks(refresh);if(observing){getContentResolver().unregisterContentObserver(observer);observing=false;}}
    @Override protected void onDestroy(){loadVersion++;worker.shutdownNow();main.removeCallbacksAndMessages(null);super.onDestroy();}
    private void seedDemo(){
        if(mode.equals("demo")&&!prefs.getBoolean("demoSeeded",false)){
            store.threads.put("demo-mom","family");store.threads.put("demo-work","work");store.threads.put("demo-friend","friends");store.save();prefs.edit().putBoolean("demoSeeded",true).apply();
        }
    }
    private boolean allowed(){return checkSelfPermission(Manifest.permission.READ_SMS)==PackageManager.PERMISSION_GRANTED;}
    private boolean smsPermissions(){
        return checkSelfPermission(Manifest.permission.SEND_SMS)==PackageManager.PERMISSION_GRANTED
                &&checkSelfPermission(Manifest.permission.READ_SMS)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean isDefaultSms(){return Build.VERSION.SDK_INT<19||getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));}
    private void handleMessagingIntent(Intent intent){
        String action=intent.getAction();Uri data=intent.getData();
        if((Intent.ACTION_SENDTO.equals(action)||Intent.ACTION_VIEW.equals(action))&&data!=null){
            String scheme=data.getScheme();if("sms".equalsIgnoreCase(scheme)||"smsto".equalsIgnoreCase(scheme)||"mms".equalsIgnoreCase(scheme)||"mmsto".equalsIgnoreCase(scheme)){
                compose(data.getSchemeSpecificPart(),intent.getStringExtra("sms_body"));intent.setData(null);intent.setAction(null);
            }
        }
    }
    private void load(){
        final int version=++loadVersion;final String source=mode;
        if(source.equals("live")&&!allowed()){
            if(observing){getContentResolver().unregisterContentObserver(observer);observing=false;}
            error="SMS access is off. Showing connected email only.";
        }
        if(source.equals("live")&&allowed()&&!observing)try{getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI,true,observer);observing=true;}catch(SecurityException ignored){}
        loading=true;error="";render();
        worker.execute(()->{
            List<SmsData.Message> result;String failure="";
            try{result=source.equals("demo")?SmsData.demo():source.equals("live")?(allowed()?SmsData.live(this):new ArrayList<>()):source.equals("email")?new ArrayList<>():SmsData.readImport(this);
                if(!source.equals("demo"))result.addAll(MailData.cached(this));result.sort((a,b)->Long.compare(b.date,a.date));
                if(source.equals("live")&&!allowed())failure="SMS access is off. Connected email is still available.";
            }
            catch(Exception e){result=source.equals("demo")?new ArrayList<>():MailData.cached(this);failure="Could not load SMS messages. "+(e instanceof SecurityException?"Android blocked SMS access. You can import an XML backup instead.":"Try refreshing or importing your backup again.");}
            final List<SmsData.Message> data=result;final String issue=failure;
            main.post(()->{if(isDestroyed()||version!=loadVersion)return;messages=data;error=issue;loading=false;render();});
        });
    }
    private void changeMode(String value){
        if(observing){getContentResolver().unregisterContentObserver(observer);observing=false;}
        mode=value;prefs.edit().putString("mode",value).apply();store=new Store(this,mode);seedDemo();
        tab="all";thread=null;selected.clear();undo=null;query="";playingMessage="";messages.clear();build();load();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout col(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private TextView text(String value,float size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(prefs.getString("font","sans-serif"),Typeface.NORMAL));return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private Button button(String title,Runnable run){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(accent);b.setTextSize(14);b.setMinHeight(dp(48));b.setOnClickListener(v->run.run());return b;}
    private void pad(View v,int n){v.setPadding(dp(n),dp(n),dp(n),dp(n));}
    private void build(){
        bg=Color.parseColor(dark?"#10131C":"#F5F4FA");card=Color.parseColor(dark?"#1B2030":"#FFFFFF");
        ink=Color.parseColor(dark?"#F2F0FC":"#232239");muted=Color.parseColor(dark?"#B9BDD0":"#646479");accent=Color.parseColor(dark?"#B6AAFF":"#635BDB");
        textSize=prefs.getFloat("size",16f);
        root=col();root.setBackgroundColor(bg);root.setPadding(dp(16),dp(12),dp(16),0);
        if(Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,in)->{android.graphics.Insets bars=in.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(dp(16)+bars.left,dp(12)+bars.top,dp(16)+bars.right,bars.bottom);return in;});
        else root.setOnApplyWindowInsetsListener((v,in)->{v.setPadding(dp(16)+in.getSystemWindowInsetLeft(),dp(12),dp(16)+in.getSystemWindowInsetRight(),0);return in;});
        setContentView(root);root.requestApplyInsets();
        LinearLayout heading=row();TextView brand=text("Haven Message",29,ink);brand.setTypeface(null,Typeface.BOLD);heading.addView(brand,new LinearLayout.LayoutParams(0,-2,1));heading.addView(button("Settings",this::settings));root.addView(heading);
        subtitle=text("",13,muted);root.addView(subtitle);root.addView(space(12));
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(ink);search.setHintTextColor(muted);search.setTextSize(16);search.setHint("Search people or messages");search.setBackground(shape(card,16));pad(search,14);search.setText(query);root.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){query=s.toString();renderList();}public void afterTextChanged(Editable e){}});
        root.addView(space(10));HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);tabs=row();scroll.addView(tabs);root.addView(scroll);
        actions=row();root.addView(actions);
        status=text("",13,muted);pad(status,8);root.addView(status);
        list=new ListView(this);list.setDivider(null);list.setClipToPadding(false);list.setPadding(0,0,0,dp(12));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=row();bottom.addView(button("Groups",this::manageGroups),new LinearLayout.LayoutParams(0,-2,1));bottom.addView(button("Rules",this::manageRules),new LinearLayout.LayoutParams(0,-2,1));bottom.addView(button("New text",()->compose("")),new LinearLayout.LayoutParams(0,-2,1));root.addView(bottom);
        render();
    }
    private View space(int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));return v;}
    private void render(){if(list==null)return;subtitle.setText(mode.equals("demo")?"SAMPLE MESSAGES · Try moving something":mode.equals("live")?"PHONE SMS + EMAIL · Your groups, your way":mode.equals("email")?"EMAIL · Gmail + Outlook":"IMPORTED SMS + EMAIL · Backup snapshot");renderTabs();renderActions();renderList();GroupWidget.snapshot(this,mode,messages,store);}
    private void renderTabs(){
        tabs.removeAllViews();addTab("all","All",accent);
        for(Store.Group g:store.groups)addTab(g.id,g.label(),Color.parseColor(g.color));
        tabs.addView(button("+ Group",()->editGroup(null)));
    }
    private void addTab(String id,String name,int color){
        Button b=button(name,()->{tab=id;thread=null;selected.clear();render();});
        b.setBackground(shape(tab.equals(id)?color:card,24));b.setTextColor(tab.equals(id)?Color.WHITE:ink);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.setMargins(0,0,dp(7),0);tabs.addView(b,p);
    }
    private void renderActions(){
        actions.removeAllViews();
        if(thread!=null)actions.addView(button("‹ Inbox",()->{thread=null;selected.clear();render();}));
        if(!selected.isEmpty()){
            actions.addView(button("Move ("+selected.size()+")",()->pickGroup(g->moveSelected(g))));
            actions.addView(button("Cancel",()->{selected.clear();render();}));
        }else if(thread!=null){
            actions.addView(button("Move chat",()->pickGroup(g->moveConversation(thread,g))));
            actions.addView(button("Reply",()->{for(SmsData.Message m:messages)if(m.thread.equals(thread)){reply(m);break;}}));
        }
        if(undo!=null)actions.addView(button("Undo",()->{try{store.restore(undo);store.save();undo=null;render();}catch(Exception e){toast("Could not undo.");}}));
    }
    static class Item {
        SmsData.Message m;int count=1;String group;
        Item(SmsData.Message m,String group){this.m=m;this.group=group;}
    }
    private void renderList(){
        if(list==null)return;
        List<Item> rows=new ArrayList<>();Map<String,Item> chats=new LinkedHashMap<>();
        String q=query.toLowerCase(Locale.ROOT);
        for(SmsData.Message m:messages){
            if(thread!=null&&!m.thread.equals(thread))continue;
            String group=store.resolve(m);
            if(thread==null&&!tab.equals("all")&&!tab.equals(group))continue;
            if(!q.isEmpty()&&!(m.name+" "+m.address+" "+m.body).toLowerCase(Locale.ROOT).contains(q))continue;
            if(thread!=null)rows.add(new Item(m,group));
            else{Item item=chats.get(m.thread);if(item==null)chats.put(m.thread,new Item(m,group));else{item.count++;if(!item.group.equals(group))item.group="mixed";}}
        }
        if(thread==null)rows.addAll(chats.values());
        status.setText(loading?"Loading messages…":!error.isEmpty()?error:rows.isEmpty()?"No messages here yet. Move a chat here or add a sorting rule.":thread==null?rows.size()+" conversations · Hold to select and move":"Full conversation · Hold a message to move it");
        list.setAdapter(new BaseAdapter(){
            public int getCount(){return rows.size();}public Object getItem(int p){return rows.get(p);}public long getItemId(int p){return p;}
            public View getView(int pos,View convert,android.view.ViewGroup parent){
                Item item=rows.get(pos);SmsData.Message m=item.m;String key=thread==null?m.thread:m.id;boolean checked=selected.contains(key);
                LinearLayout wrap=col();wrap.setPadding(0,dp(4),0,dp(4));LinearLayout box=col();pad(box,16);
                org.json.JSONObject style=Appearance.get(MainActivity.this,mode,m.id);
                int foreground=Appearance.color(style,"text",ink);
                if(checked)box.setBackground(shape(dark?0xDD373155:0xDDE8E2FF,18));
                BubbleFrame bubble=new BubbleFrame(MainActivity.this,style,card,thread!=null&&m.id.equals(playingMessage));
                LinearLayout top=row();TextView name=text((checked?"✓  ":"")+(thread==null?m.name:m.sent?"You":m.name),textSize,foreground);name.setTypeface(null,Typeface.BOLD);top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
                TextView time=text(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(m.date)),11,foreground);time.setMaxWidth(dp(120));top.addView(time);box.addView(top);
                TextView body=text((thread==null&&m.sent?"You: ":"")+m.body,style.optInt("size",(int)textSize-1),foreground);body.setTypeface(Appearance.font(MainActivity.this,style));body.setPadding(0,dp(7),0,dp(9));if(thread==null){body.setMaxLines(2);body.setEllipsize(TextUtils.TruncateAt.END);}else body.setTextIsSelectable(false);box.addView(body);
                String label=item.group.equals("mixed")?"Multiple groups":store.group(item.group).label();
                TextView badge=text(m.source+" · "+label+(thread==null?"  ·  "+item.count+" messages":""),12,foreground);box.addView(badge);
                bubble.addView(box,new FrameLayout.LayoutParams(-1,-2));wrap.addView(bubble,new LinearLayout.LayoutParams(-1,-2));return wrap;
            }
        });
        list.setOnItemClickListener((parent,v,pos,id)->{SmsData.Message m=rows.get(pos).m;if(!selected.isEmpty()){toggle(thread==null?m.thread:m.id);return;}if(thread==null){thread=m.thread;render();}else messageMenu(m);});
        list.setOnItemLongClickListener((parent,v,pos,id)->{SmsData.Message m=rows.get(pos).m;toggle(thread==null?m.thread:m.id);return true;});
    }
    private void toggle(String id){if(!selected.add(id))selected.remove(id);renderActions();renderList();}
    private interface Chosen{void run(String id);}
    private void pickGroup(Chosen chosen){String[] names=new String[store.groups.size()];for(int i=0;i<names.length;i++)names[i]=store.groups.get(i).label();new AlertDialog.Builder(this).setTitle("Move to group").setItems(names,(d,i)->chosen.run(store.groups.get(i).id)).setNegativeButton("Cancel",null).show();}
    private void moveSelected(String group){undo=store.snapshot();if(thread==null)for(String id:selected)Grouping.moveThread(id,group,store.threads,store.messages);else for(String id:selected)store.messages.put(id,group);store.save();selected.clear();render();toast("Moved to "+store.group(group).name);}
    private void moveConversation(String id,String group){undo=store.snapshot();Grouping.moveThread(id,group,store.threads,store.messages);store.save();selected.clear();render();toast("Conversation moved. Future texts follow this group.");}
    private void messageMenu(SmsData.Message m){
        new AlertDialog.Builder(this).setTitle(m.name).setItems(new String[]{"Move this message","Move entire conversation","Use conversation group for this message","Use rules for entire conversation","Copy text","Customize this message","Play / stop video background","Remind me to reply"},(d,i)->{
            if(i==0)pickGroup(g->{undo=store.snapshot();store.messages.put(m.id,g);store.save();render();});
            if(i==1)pickGroup(g->moveConversation(m.thread,g));
            if(i==2){undo=store.snapshot();store.messages.remove(m.id);store.save();render();}
            if(i==3){undo=store.snapshot();store.threads.remove(m.thread);store.messages.keySet().removeIf(k->k.startsWith(m.thread+":"));store.save();render();}
            if(i==4){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Text message",m.body));toast("Copied");}
            if(i==5)startActivity(new Intent(this,StyleActivity.class).putExtra("mode",mode).putExtra("id",m.id).putExtra("body",m.body));
            if(i==6){org.json.JSONObject style=Appearance.get(this,mode,m.id);if(!style.optString("kind").equals("video")){toast("Choose a video in Customize this message first.");return;}playingMessage=playingMessage.equals(m.id)?"":m.id;renderList();}
            if(i==7)remind(m);
        }).show();
    }
    private void reply(SmsData.Message m){if(mode.equals("demo")){toast("Sample messages are fictional. Switch to Phone SMS to reply.");return;}if(m.source.equals("SMS")){compose(m.address,"");return;}try{
        Uri url=Uri.parse(m.web);String host=url.getHost();
        if("https".equals(url.getScheme())&&host!=null&&(host.equals("mail.google.com")||host.equals("outlook.live.com")||host.equals("outlook.office.com")||host.equals("outlook.office365.com")))startActivity(new Intent(Intent.ACTION_VIEW,url));
        else startActivity(new Intent(Intent.ACTION_SENDTO,Uri.fromParts("mailto",m.address,null)).putExtra(Intent.EXTRA_SUBJECT,"Re: "+m.subject));
    }catch(ActivityNotFoundException e){toast("Install an email app or browser to reply.");}}
    private void remind(SmsData.Message m){
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},104);toast("Allow notifications, then choose Remind me to reply again.");return;}
        if(!((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled()){new AlertDialog.Builder(this).setMessage("Turn on Haven Message notifications in App settings to receive reminder alerts.").setPositiveButton("Settings",(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName()))).setNegativeButton("Cancel",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Remind me to reply").setItems(new String[]{"In 15 minutes","In 1 hour","Tomorrow at 9 AM","Choose date and time"},(d,i)->{
            Calendar when=Calendar.getInstance();if(i==0)when.add(Calendar.MINUTE,15);if(i==1)when.add(Calendar.HOUR_OF_DAY,1);if(i==2){when.add(Calendar.DATE,1);when.set(Calendar.HOUR_OF_DAY,9);when.set(Calendar.MINUTE,0);when.set(Calendar.SECOND,0);}
            if(i<3){saveReminder(m,when.getTimeInMillis());return;}
            new DatePickerDialog(this,(p,y,mo,day)->{when.set(y,mo,day);new TimePickerDialog(this,(tp,h,min)->{when.set(Calendar.HOUR_OF_DAY,h);when.set(Calendar.MINUTE,min);when.set(Calendar.SECOND,0);saveReminder(m,when.getTimeInMillis());},when.get(Calendar.HOUR_OF_DAY),when.get(Calendar.MINUTE),android.text.format.DateFormat.is24HourFormat(this)).show();},when.get(Calendar.YEAR),when.get(Calendar.MONTH),when.get(Calendar.DAY_OF_MONTH)).show();
        }).show();
    }
    private void saveReminder(SmsData.Message m,long time){if(time<=System.currentTimeMillis()){toast("Choose a future time.");return;}try{Reminders.add(this,mode,m,time);toast("Reminder saved. Android may delay the alert during power-saving.");}catch(Exception e){toast("Could not save this reminder.");}}
    private void compose(String address){compose(address,"");}
    private void compose(String address,String body){
        if(mode.equals("demo")){new AlertDialog.Builder(this).setTitle("Sample mode").setMessage("Sample contacts are fictional. Switch to Phone SMS in Settings to reply or start a real text.").setPositiveButton("Connect SMS",(d,w)->connect()).setNegativeButton("Close",null).show();return;}
        if(!isDefaultSms()){
            new AlertDialog.Builder(this).setTitle("Make Haven Message your texting app?").setMessage("Android only allows a complete SMS app to receive and save texts reliably. Choose Haven Message as your default SMS app, then open Reply or New text again.").setPositiveButton("Set as default",(d,w)->requestDefaultSms()).setNegativeButton("Close",null).show();return;
        }
        showComposer(address,body);
    }
    private void showComposer(String address,String body){
        LinearLayout form=col();pad(form,20);
        EditText to=field(form,"Phone number",address==null?"":address);to.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        EditText message=field(form,"Message",body==null?"":body);message.setSingleLine(false);message.setMinLines(4);message.setGravity(Gravity.TOP);message.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(text("SMS only for now · MMS/RCS attachments are not sent by this build.",12,muted));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(address==null||address.isEmpty()?"New text":"Reply").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Send",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String recipient=to.getText().toString().trim(),textValue=message.getText().toString().trim();
            if(recipient.isEmpty()){to.setError("Enter a phone number");return;}if(textValue.isEmpty()){message.setError("Enter a message");return;}
            if(!smsPermissions()){pendingSendAddress=recipient;pendingSendBody=textValue;requestSmsPermissions();dialog.dismiss();return;}
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);sendSms(recipient,textValue,dialog);
        }));dialog.show();
    }
    private void sendSms(String address,String body,AlertDialog dialog){
        SmsSender.send(this,address,body,(ok,errorMessage)->main.post(()->{
            if(ok){toast("Text sent");if(dialog!=null)dialog.dismiss();if(!mode.equals("live"))changeMode("live");else load();}
            else{if(dialog!=null)dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);new AlertDialog.Builder(this).setTitle("Text not sent").setMessage(errorMessage).setPositiveButton("OK",null).show();}
        }));
    }
    private void requestSmsPermissions(){
        List<String> missing=new ArrayList<>();for(String p:new String[]{Manifest.permission.READ_SMS,Manifest.permission.RECEIVE_SMS,Manifest.permission.SEND_SMS,Manifest.permission.WRITE_SMS})if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)missing.add(p);
        if(missing.isEmpty()){if(!pendingSendAddress.isEmpty()){String a=pendingSendAddress,b=pendingSendBody;pendingSendAddress="";pendingSendBody="";sendSms(a,b,null);}else changeMode("live");return;}
        requestPermissions(missing.toArray(new String[0]),REQUEST_SMS_PERMISSIONS);
    }
    private void requestDefaultSms(){
        if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_SMS)&&!roles.isRoleHeldBySelf())startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_SMS),REQUEST_DEFAULT_SMS);else toast("Android does not offer the SMS role on this device.");}
        else {Intent change=new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,getPackageName());startActivityForResult(change,REQUEST_DEFAULT_SMS);}
    }
    private void defaultSmsSettings(){if(isDefaultSms())requestSmsPermissions();else requestDefaultSms();}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private EditText field(LinearLayout form,String label,String value){form.addView(text(label,13,muted));EditText e=new EditText(this);e.setText(value);e.setTextColor(ink);e.setSingleLine(true);form.addView(e);return e;}
    private void manageGroups(){
        String[] names=new String[store.groups.size()+1];for(int i=0;i<store.groups.size();i++)names[i]=store.groups.get(i).label();names[names.length-1]="+ Create group";
        new AlertDialog.Builder(this).setTitle("Your groups").setItems(names,(d,i)->{if(i==store.groups.size()){editGroup(null);return;}Store.Group g=store.groups.get(i);
            new AlertDialog.Builder(this).setTitle(g.label()).setItems(new String[]{"Edit name, icon & color","Move tab left","Move tab right","Delete group"},(x,action)->{
                if(action==0)editGroup(g);
                if(action==1||action==2){int index=store.groups.indexOf(g),next=index+(action==1?-1:1);if(next>=0&&next<store.groups.size()){undo=store.snapshot();Collections.swap(store.groups,index,next);store.save();render();}}
                if(action==3){if(g.id.equals("other")){toast("Keep one catch-all group. You can rename and recolor it.");return;}
                    new AlertDialog.Builder(this).setTitle("Delete "+g.name+"?").setMessage("Messages stay on your phone. Assigned texts move to "+store.group("other").name+". Rules for this group are removed.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(y,z)->{undo=store.snapshot();store.delete(g.id);if(tab.equals(g.id))tab="all";render();}).show();}
            }).show();
        }).setNegativeButton("Close",null).show();
    }
    private void editGroup(Store.Group existing){
        LinearLayout form=col();pad(form,20);EditText name=field(form,"Group name",existing==null?"":existing.name),icon=field(form,"Icon or emoji",existing==null?"●":existing.icon),color=field(form,"Color · #RRGGBB",existing==null?"#635BDB":existing.color);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?"Create a group":"Customize group").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
            String n=name.getText().toString().trim(),ic=icon.getText().toString().trim(),c=color.getText().toString().trim();
            if(n.isEmpty()||n.length()>32){name.setError("Use 1–32 characters");return;}if(ic.isEmpty()||ic.codePointCount(0,ic.length())>8){icon.setError("Use an icon or up to 8 characters");return;}
            if(!c.matches("#[0-9a-fA-F]{6}")){color.setError("Example: #635BDB");return;}
            for(Store.Group g:store.groups)if(g!=existing&&g.name.equalsIgnoreCase(n)){name.setError("That name is already used");return;}
            undo=store.snapshot();if(existing==null)store.groups.add(new Store.Group(UUID.randomUUID().toString(),n,ic,c));else{existing.name=n;existing.icon=ic;existing.color=c;}store.save();render();dialog.dismiss();
        }));dialog.show();
    }
    private void manageRules(){
        String[] titles=new String[store.rules.size()+1];for(int i=0;i<store.rules.size();i++){Grouping.Rule r=store.rules.get(i);titles[i]=(i+1)+". "+(r.kind.equals("sender")?"Sender ":"Contains “")+r.match+(r.kind.equals("sender")?"":"”")+" → "+store.group(r.group).name;}titles[titles.length-1]="+ Add sorting rule";
        new AlertDialog.Builder(this).setTitle("Rules · first match wins").setItems(titles,(d,i)->{
            if(i==store.rules.size()){editRule(null);return;}Grouping.Rule r=store.rules.get(i);
            new AlertDialog.Builder(this).setTitle("Rule options").setItems(new String[]{"Edit rule","Raise priority","Lower priority","Delete rule"},(x,k)->{
                if(k==0){editRule(r);return;}undo=store.snapshot();if(k==3)store.rules.remove(r);else{int next=i+(k==1?-1:1);if(next>=0&&next<store.rules.size())Collections.swap(store.rules,i,next);}store.save();render();
            }).show();
        }).setNegativeButton("Close",null).show();
    }
    private void editRule(Grouping.Rule existing){
        LinearLayout form=col();pad(form,20);form.addView(text("Manual moves always take priority.",14,muted));Spinner kind=new Spinner(this);kind.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Exact sender number / ID","Message contains keyword"}));form.addView(kind);if(existing!=null)kind.setSelection(existing.kind.equals("sender")?0:1);
        EditText match=field(form,"Number (including country code) or keyword",existing==null?"":existing.match);Spinner group=new Spinner(this);String[] names=new String[store.groups.size()];for(int i=0;i<names.length;i++)names[i]=store.groups.get(i).label();group.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));form.addView(group);if(existing!=null)for(int i=0;i<store.groups.size();i++)if(store.groups.get(i).id.equals(existing.group))group.setSelection(i);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Sorting rule").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String value=match.getText().toString().trim();if(value.isEmpty()){match.setError("Enter a number or keyword");return;}undo=store.snapshot();Grouping.Rule r=new Grouping.Rule(kind.getSelectedItemPosition()==0?"sender":"keyword",value,store.groups.get(group.getSelectedItemPosition()).id);if(existing==null)store.rules.add(r);else store.rules.set(store.rules.indexOf(existing),r);store.save();render();dialog.dismiss();}));dialog.show();
    }
    private void settings(){
        String[] options={"Connect phone SMS","Show sample messages","Import SMS backup (XML)","Open saved import","Refresh messages","Theme: "+(dark?"dark":"light"),"Text size","Optional contact names","How it works","Gmail + Outlook accounts","Email inbox","Reply reminders","Default text font","Home-screen widgets","Make Haven Message the default SMS app"};
        new AlertDialog.Builder(this).setTitle("Make it yours").setItems(options,(d,i)->{
            if(i==0)connect();if(i==1)changeMode("demo");if(i==2)importBackup();if(i==3)changeMode("import");if(i==4)load();
            if(i==5){prefs.edit().putBoolean("dark",!dark).apply();recreate();}
            if(i==6)new AlertDialog.Builder(this).setTitle("Text size").setItems(new String[]{"Compact","Comfortable","Large"},(x,k)->{prefs.edit().putFloat("size",new float[]{14,16,20}[k]).apply();build();}).show();
            if(i==7)requestPermissions(new String[]{Manifest.permission.READ_CONTACTS},102);
            if(i==9)startActivity(new Intent(this,AccountsActivity.class));if(i==10)changeMode("email");if(i==11)startActivity(new Intent(this,ReminderActivity.class));
            if(i==12)new AlertDialog.Builder(this).setTitle("Default font").setItems(Appearance.FONTS,(d2,k)->{prefs.edit().putString("font",Appearance.FONTS[k]).apply();build();}).show();
            if(i==13)new AlertDialog.Builder(this).setTitle("Add a group widget").setMessage("Hold an empty space on your home screen, choose Widgets, then Haven Message. Choose a message source and group, background color, and whether previews are visible. Tap the widget title to refresh its messages in the app; tap its small subtitle to change its settings.").setPositiveButton("OK",null).show();
            if(i==14)defaultSmsSettings();
            if(i==8)new AlertDialog.Builder(this).setTitle("Your texts. Your tabs.").setMessage("Hold conversations to select several and move them. Open a conversation and hold individual messages to move only those. Tap a message for more controls.\n\nTabs show conversations with messages in that group. Opening one shows its full history. Moving a whole conversation clears its individual overrides and applies to future SMS too.\n\nRules run in order. Your manual moves override them. The app uses exact sender or keyword rules; it does not guess who is family.\n\nMake Haven Message the default SMS app to reply and start new SMS messages inside Haven Message. Incoming SMS are saved and notified here. MMS media and RCS are not parsed or sent by this build. Connected Gmail and Outlook messages still use the provider or an email app for replies.\n\nGroups, message appearance and reminders stay on your phone. Optional Gmail and Outlook connections contact Google/Microsoft directly; there is no Haven Message server or analytics. Photo/video backgrounds are local decoration, not outgoing attachments. Samples, imported backups and phone SMS keep separate grouping settings. Uninstalling clears this app’s groups and imports; original phone messages stay untouched.").setPositiveButton("Got it",null).show();
        }).setNegativeButton("Close",null).show();
    }
    private void connect(){
        new AlertDialog.Builder(this).setTitle("Connect phone SMS").setMessage("Haven Message needs to be your default SMS app to receive texts, save incoming messages, and send replies from inside Haven Message. It also needs SMS permissions. Your existing SMS database is not deleted.").setNegativeButton("Cancel",null).setPositiveButton("Continue",(d,i)->{if(!isDefaultSms())requestDefaultSms();else requestSmsPermissions();}).show();
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==REQUEST_SMS_PERMISSIONS){
            if(!smsPermissions()){new AlertDialog.Builder(this).setTitle("SMS permissions needed").setMessage("Haven Message cannot send or read phone texts until SMS permissions are allowed. You can enable them in App permissions.").setPositiveButton("App permissions",(d,i)->startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).setNegativeButton("Close",null).show();return;}
            if(!pendingSendAddress.isEmpty()){String a=pendingSendAddress,b=pendingSendBody;pendingSendAddress="";pendingSendBody="";sendSms(a,b,null);}else changeMode("live");
        }
        if(request==102)load();
    }
    private void importBackup(){
        new AlertDialog.Builder(this).setTitle("Import SMS backup").setMessage("Choose an XML file with <smses> and <sms> records. This replaces the previous imported snapshot only. It does not write messages to your phone. MMS and RCS are not imported.").setNegativeButton("Cancel",null).setPositiveButton("Choose file",(d,i)->{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,103);}).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent intent){
        super.onActivityResult(request,result,intent);
        if(request==REQUEST_DEFAULT_SMS){if(isDefaultSms())requestSmsPermissions();else toast("Haven Message was not selected as the default SMS app.");return;}
        if(request!=103||result!=RESULT_OK||intent==null||intent.getData()==null)return;Uri uri=intent.getData();
        ProgressDialog progress=ProgressDialog.show(this,"Importing","Reading your SMS backup…",true,false);
        worker.execute(()->{String problem=null;try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new java.io.IOException("File could not be opened");SmsData.saveImport(this,SmsData.importXml(in));}catch(Exception e){problem="Could not import this file. Use a valid SMS XML backup with fewer than 100,000 messages.";}final String failure=problem;main.post(()->{if(isDestroyed())return;progress.dismiss();if(failure==null)changeMode("import");else new AlertDialog.Builder(this).setTitle("Import failed").setMessage(failure).setPositiveButton("OK",null).show();});});
    }
    @Override public void onBackPressed(){if(!selected.isEmpty()){selected.clear();render();}else if(thread!=null){thread=null;render();}else super.onBackPressed();}
}
