package com.jai.threadtabs;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.util.Xml;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

final class SmsData {
    static class Message {
        final String id,thread,address,name,body;
        String source="SMS",account="",subject="",web="";
        final long date;
        final boolean sent;
        Message(String id,String thread,String address,String name,String body,long date,boolean sent) {
            this.id=thread+":"+id;this.thread=thread;this.address=address;this.name=name;this.body=body;this.date=date;this.sent=sent;
        }
    }
    static List<Message> live(Context c) {
        List<Message> list=new ArrayList<>();Map<String,String> names=new HashMap<>();
        String[] cols={"_id","thread_id","address","body","date","type"};
        try(Cursor cur=c.getContentResolver().query(Telephony.Sms.CONTENT_URI,cols,"type IN (1,2)",null,"date DESC")) {
            if(cur==null)throw new IllegalStateException("The phone did not return its SMS database.");
            while(cur.moveToNext()) {
                if(Thread.currentThread().isInterrupted())throw new IllegalStateException("Loading canceled");
                String address=cur.isNull(2)?"Unknown":cur.getString(2);
                String name=names.get(address);
                if(name==null){ name=contact(c,address);names.put(address,name); }
                list.add(new Message(cur.getString(0),"sms-"+cur.getString(1),address,name,cur.isNull(3)?"":cur.getString(3),cur.getLong(4),cur.getInt(5)==2));
            }
        }
        return list;
    }
    private static String contact(Context c,String address) {
        if(c.checkSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)return address;
        try(Cursor cur=c.getContentResolver().query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(address)),new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},null,null,null)) {
            if(cur!=null&&cur.moveToFirst())return cur.getString(0);
        } catch(Exception ignored) {}
        return address;
    }
    static List<Message> demo() {
        long now=System.currentTimeMillis();List<Message> list=new ArrayList<>();
        list.add(new Message("1","demo-mom","+15550100101","Mom","Dinner Sunday? I’m making your favorite. ♥",now-120000,false));
        list.add(new Message("2","demo-mom","+15550100101","Mom","I’ll bring dessert!",now-90000,true));
        list.add(new Message("3","demo-work","+15550100102","Morgan · Work","Can you look at the server logs before our 10am meeting?",now-180000,false));
        list.add(new Message("4","demo-work","+15550100102","Morgan · Work","Also, are you coming to the cookout Saturday?",now-160000,false));
        list.add(new Message("5","demo-friend","+15550100103","Alex","Game night Friday? 🎮",now-700000,false));
        list.add(new Message("6","demo-other","5550104","5550104","Your pickup is ready at the front desk.",now-1000000,false));
        Message email=new Message("7","demo-gmail","family@example.com","Jordan · Gmail","Sunday plans\n\nI emailed the photos from our family trip.",now-220000,false);email.source="Gmail";email.subject="Sunday plans";email.account="sample@example.com";list.add(email);
        Message outlook=new Message("8","demo-outlook","morgan@example.com","Morgan · Outlook","Project notes\n\nHere are the meeting notes for tomorrow.",now-300000,false);outlook.source="Outlook";outlook.subject="Project notes";outlook.account="sample@example.com";list.add(outlook);
        list.sort((a,b)->Long.compare(b.date,a.date));return list;
    }
    static List<Message> importXml(InputStream in) throws Exception {
        XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false);
        p.setInput(in,null);List<Message> result=new ArrayList<>();Set<String> seen=new HashSet<>();boolean root=false;
        for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()) {
            if(event==XmlPullParser.START_TAG&&p.getDepth()==1){if(!"smses".equals(p.getName()))throw new IOException("Expected an <smses> SMS backup.");root=true;}
            if(event!=XmlPullParser.START_TAG||!p.getName().equals("sms"))continue;
            String a=attr(p,"address","Unknown"),b=attr(p,"body",""),name=attr(p,"contact_name",a);
            long date=Long.parseLong(attr(p,"date","0"));String type=attr(p,"type","1");
            if(!type.equals("1")&&!type.equals("2"))continue;
            if(name.equals("(Unknown)")||name.equals("null"))name=a;
            String key=hash(a+"\u0000"+date+"\u0000"+type+"\u0000"+b);
            if(seen.add(key))result.add(new Message(key,"import-"+Grouping.normalize(a),a,name,b,date,type.equals("2")));
            if(result.size()>100000)throw new IOException("Please use a backup with fewer than 100,000 SMS messages.");
        }
        if(!root)throw new IOException("The file is empty.");
        if(result.isEmpty())throw new IOException("This backup has no incoming or sent SMS messages.");
        result.sort((a,b)->Long.compare(b.date,a.date));return result;
    }
    private static String attr(XmlPullParser p,String key,String fallback){String v=p.getAttributeValue(null,key);return v==null?fallback:v;}
    private static String hash(String s) throws Exception { byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte x:b)out.append(String.format(Locale.ROOT,"%02x",x&255));return out.toString(); }
    static void saveImport(Context c,List<Message> list) throws Exception {
        JSONArray a=new JSONArray();for(Message m:list)a.put(new JSONObject().put("id",m.id.substring(m.thread.length()+1)).put("thread",m.thread).put("address",m.address).put("name",m.name).put("body",m.body).put("date",m.date).put("sent",m.sent));
        android.util.AtomicFile f=new android.util.AtomicFile(new File(c.getFilesDir(),"import.json"));FileOutputStream out=null;
        try {out=f.startWrite();out.write(a.toString().getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(Exception e){if(out!=null)f.failWrite(out);throw e;}
    }
    static List<Message> readImport(Context c) throws Exception {
        android.util.AtomicFile f=new android.util.AtomicFile(new File(c.getFilesDir(),"import.json"));
        JSONArray a=new JSONArray(new String(f.readFully(),StandardCharsets.UTF_8));List<Message> list=new ArrayList<>();
        for(int i=0;i<a.length();i++){JSONObject m=a.getJSONObject(i);list.add(new Message(m.getString("id"),m.getString("thread"),m.getString("address"),m.getString("name"),m.getString("body"),m.getLong("date"),m.getBoolean("sent")));}return list;
    }
}
