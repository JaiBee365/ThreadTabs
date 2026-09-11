package com.jai.threadtabs;
import java.util.*;
public class GroupingTest {
    static int checks=0;
    static void eq(Object actual,Object expected){checks++;if(!Objects.equals(actual,expected))throw new AssertionError("Expected "+expected+", got "+actual);}
    public static void main(String[] args){
        Map<String,String> msgs=new HashMap<>(),threads=new HashMap<>();
        List<Grouping.Rule> rules=new ArrayList<>();Set<String> valid=new HashSet<>(Arrays.asList("family","work","friends","other"));
        rules.add(new Grouping.Rule("sender","+1 (555) 111-2222","family"));
        rules.add(new Grouping.Rule("keyword","meeting","work"));
        eq(Grouping.resolve("t:1","t","+15551112222","MEETING",msgs,threads,rules,valid),"family");
        eq(Grouping.resolve("t:1","t","5550000","MEETING",msgs,threads,rules,valid),"work");
        eq(Grouping.resolve("t:1","t","5550000","Hello",msgs,threads,rules,valid),"other");
        threads.put("t","friends");
        eq(Grouping.resolve("t:1","t","+15551112222","MEETING",msgs,threads,rules,valid),"friends");
        msgs.put("t:1","work");
        eq(Grouping.resolve("t:1","t","+15551112222","Hi",msgs,threads,rules,valid),"work");
        msgs.put("t:2","family");msgs.put("t2:1","family");
        Grouping.moveThread("t","other",threads,msgs);
        eq(msgs.containsKey("t:1"),false);eq(msgs.containsKey("t:2"),false);eq(msgs.get("t2:1"),"family");
        eq(Grouping.resolve("t:99","t","+15551112222","meeting",msgs,threads,rules,valid),"other");
        threads.remove("t");msgs.put("t:1","deleted");
        eq(Grouping.resolve("t:1","t","+15551112222","Hi",msgs,threads,rules,valid),"family");
        eq(Grouping.normalize(" ACME "),"acme");eq(Grouping.normalize("+44 (20) 1234-5678"),"+442012345678");
        rules.add(0,new Grouping.Rule("keyword","","friends"));
        eq(Grouping.resolve("x:1","x","unknown","Nothing",msgs,threads,rules,valid),"other");
        // Country codes are not discarded, avoiding accidental merging of distinct numbers.
        eq(Grouping.resolve("x:1","x","15551112222","Nothing",msgs,threads,rules,valid),"other");
        System.out.println("Passed "+checks+" grouping checks.");
    }
}
