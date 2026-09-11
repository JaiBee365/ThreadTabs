package com.jai.threadtabs;

import android.content.*;
import org.json.*;
import java.util.*;

final class Store {
    static class Group {
        String id,name,icon,color;
        Group(String id,String name,String icon,String color) { this.id=id;this.name=name;this.icon=icon;this.color=color; }
        String label() { return icon+" "+name; }
    }
    final List<Group> groups=new ArrayList<>();
    final List<Grouping.Rule> rules=new ArrayList<>();
    final Map<String,String> messages=new HashMap<>(), threads=new HashMap<>();
    private final android.content.SharedPreferences prefs;
    Store(Context c,String mode) {
        prefs=c.getSharedPreferences("groups_"+mode,Context.MODE_PRIVATE);
        String raw=prefs.getString("state","");
        if(!raw.isEmpty()) try { restore(raw); } catch(JSONException ignored) { groups.clear(); }
        if(groups.isEmpty()) {
            groups.add(new Group("family","Family","♥","#C54C76"));
            groups.add(new Group("work","Work","▣","#3C77C4"));
            groups.add(new Group("friends","Friends","☀","#AA7200"));
            groups.add(new Group("other","Other","●","#68788D"));
        }
    }
    Set<String> ids() { Set<String> s=new HashSet<>(); for(Group g:groups)s.add(g.id); return s; }
    Group group(String id) { for(Group g:groups)if(g.id.equals(id))return g; return groups.get(groups.size()-1); }
    String resolve(SmsData.Message m) { return Grouping.resolve(m.id,m.thread,m.address,m.body,messages,threads,rules,ids()); }
    void save() { prefs.edit().putString("state",snapshot()).apply(); }
    String snapshot() {
        try {
            JSONObject o=new JSONObject(); JSONArray gs=new JSONArray(),rs=new JSONArray();
            for(Group g:groups)gs.put(new JSONObject().put("id",g.id).put("name",g.name).put("icon",g.icon).put("color",g.color));
            for(Grouping.Rule r:rules)rs.put(new JSONObject().put("kind",r.kind).put("match",r.match).put("group",r.group));
            return o.put("groups",gs).put("rules",rs).put("messages",new JSONObject(messages)).put("threads",new JSONObject(threads)).toString();
        } catch(JSONException e) { throw new IllegalStateException(e); }
    }
    void restore(String raw) throws JSONException {
        JSONObject o=new JSONObject(raw); groups.clear();rules.clear();messages.clear();threads.clear();
        JSONArray gs=o.getJSONArray("groups"),rs=o.getJSONArray("rules");
        for(int i=0;i<gs.length();i++){JSONObject g=gs.getJSONObject(i);groups.add(new Group(g.getString("id"),g.getString("name"),g.getString("icon"),g.getString("color")));}
        for(int i=0;i<rs.length();i++){JSONObject r=rs.getJSONObject(i);rules.add(new Grouping.Rule(r.getString("kind"),r.getString("match"),r.getString("group")));}
        readMap(o.getJSONObject("messages"),messages);readMap(o.getJSONObject("threads"),threads);
    }
    private void readMap(JSONObject o,Map<String,String> map) throws JSONException { Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();map.put(k,o.getString(k));} }
    void delete(String id) {
        if(id.equals("other"))return;
        groups.removeIf(g->g.id.equals(id));rules.removeIf(r->r.group.equals(id));
        messages.replaceAll((k,v)->v.equals(id)?"other":v);threads.replaceAll((k,v)->v.equals(id)?"other":v);save();
    }
}
