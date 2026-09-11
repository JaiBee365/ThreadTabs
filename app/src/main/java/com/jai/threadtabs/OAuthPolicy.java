package com.jai.threadtabs;
import java.security.*;import java.nio.charset.StandardCharsets;import java.util.Base64;
final class OAuthPolicy {
    static String nonce(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    static String challenge(String verifier){try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    static boolean valid(String actual,String expected,long created,long now){return actual!=null&&expected!=null&&!expected.isEmpty()&&now>=created&&now-created<=600000&&MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8));}
}
