package com.jai.threadtabs;
public class OAuthPolicyTest {
    private static int n=0;private static void check(boolean v){n++;if(!v)throw new AssertionError("OAuth policy check "+n+" failed");}
    public static void main(String[] args){
        check(OAuthPolicy.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk").equals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"));
        check(OAuthPolicy.valid("state","state",1000,2000));check(!OAuthPolicy.valid("wrong","state",1000,2000));
        check(!OAuthPolicy.valid(null,"state",1000,2000));check(!OAuthPolicy.valid("state","state",1000,601001));check(!OAuthPolicy.valid("state","state",1000,999));
        String a=OAuthPolicy.nonce(),b=OAuthPolicy.nonce();check(a.matches("[A-Za-z0-9_-]{43}"));check(!a.equals(b));
        System.out.println("Passed "+n+" OAuth/PKCE checks.");
    }
}
