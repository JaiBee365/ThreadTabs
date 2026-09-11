package com.jai.threadtabs;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Run on Android's runtime; android.util.Xml is not a desktop JVM implementation. */
public class ImportTest {
    static List<SmsData.Message> parse(String s) throws Exception {return SmsData.importXml(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));}
    static void require(boolean v,String label){if(!v)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        String a="<sms address='+15550100101' date='1000' type='1' body='A &amp; B' contact_name='Mom'/>";
        String b="<sms address='+15550100101' date='2000' type='2' body='Reply'/>";
        List<SmsData.Message> result=parse("<smses>"+a+a+b+"<mms/><sms type='3'/></smses>");
        require(result.size()==2,"Duplicate records and non-SMS records skipped");
        require(result.get(0).sent&&result.get(0).date==2000,"Outgoing record and date order");
        require(result.get(1).body.equals("A & B"),"XML text decoding");
        require(result.get(0).thread.equals(result.get(1).thread),"Imported conversation identity");
        require(result.get(1).id.equals(parse("<smses>"+a+"</smses>").get(0).id),"Stable reimport identity");
        for(String bad:new String[]{"<wrong/>","<smses/>","<smses><sms date='oops'/></smses>"}){
            boolean failed=false;try{parse(bad);}catch(Exception e){failed=true;}require(failed,"Invalid backup rejected");
        }
        System.out.println("Passed 8 Android XML import checks.");
    }
}
