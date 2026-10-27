package com.notiurba.foto;

import java.util.*;

public final class CloudinarySignatureCheck {
    private static int checks;
    public static void main(String[] args) {
        run();
        System.out.println("UTC="+java.time.Instant.now()+" signature_checks="+checks+" failed=0");
    }
    static void run() {
        checks=0;
        String digest = CloudinarySignature.sha256(Map.of("timestamp","1315060510"),"abcd");
        require(digest.equals("5652e549a70bdc03f73a633a23b7d3f3b067d72fff26dd15b25997f46fdf6439"));
        var a = new LinkedHashMap<String,String>(); a.put("type","authenticated"); a.put("timestamp","1");
        var b = new LinkedHashMap<String,String>(); b.put("timestamp","1"); b.put("type","authenticated");
        require(CloudinarySignature.sha256(a,"abcd").equals(CloudinarySignature.sha256(b,"abcd")));
        b.put("type","upload");
        require(!CloudinarySignature.sha256(a,"abcd").equals(CloudinarySignature.sha256(b,"abcd")));
        require(digest.matches("[a-f0-9]{64}"));
        reject(() -> CloudinarySignature.sha256(Map.of("file","photo"),"abcd"));
        reject(() -> CloudinarySignature.sha256(Map.of("api_key","example"),"abcd"));
        reject(() -> CloudinarySignature.sha256(Map.of("timestamp","1")," "));
        reject(() -> CloudinarySignature.sha256(Map.of("timestamp","1"),null));
    }
    private static void require(boolean ok) { if (!ok) throw new AssertionError("Firma incorrecta"); checks++; }
    private static void reject(Runnable action) {
        try { action.run(); } catch(IllegalArgumentException e) { checks++; return; }
        throw new AssertionError("Parámetro inválido aceptado");
    }
}
