package com.jai.threadtabs;

import java.util.*;

/** Pure Java policy: message override > conversation override > first rule > Other. */
public final class Grouping {
    public static final String OTHER = "other";
    public static class Rule {
        public String kind, match, group;
        public Rule(String kind, String match, String group) { this.kind=kind; this.match=match; this.group=group; }
    }
    public static String normalize(String address) {
        if (address == null) return "";
        String trimmed=address.trim();
        if (trimmed.matches("[+0-9() .\\-]+")) return trimmed.replaceAll("[^+0-9]", "");
        return trimmed.toLowerCase(Locale.ROOT);
    }
    public static String resolve(String id, String thread, String sender, String body,
            Map<String,String> messages, Map<String,String> threads, List<Rule> rules, Set<String> valid) {
        String g=messages.get(id);
        if(valid.contains(g)) return g;
        g=threads.get(thread);
        if(valid.contains(g)) return g;
        for(Rule r:rules) {
            if(!valid.contains(r.group) || r.match.trim().isEmpty()) continue;
            boolean matches=r.kind.equals("sender") ? normalize(sender).equals(normalize(r.match))
                : body.toLowerCase(Locale.ROOT).contains(r.match.toLowerCase(Locale.ROOT));
            if(matches) return r.group;
        }
        return OTHER;
    }
    public static void moveThread(String thread,String group,Map<String,String> threads,Map<String,String> messages) {
        threads.put(thread,group);
        messages.keySet().removeIf(id -> id.startsWith(thread+":"));
    }
    private Grouping() {}
}
