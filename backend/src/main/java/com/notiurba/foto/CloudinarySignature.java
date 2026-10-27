package com.notiurba.foto;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

/** SHA-256 authentication signature over signable fields, before form encoding. */
public final class CloudinarySignature {
    private CloudinarySignature() {}
    public static String sha256(Map<String,String> signable, String secret) {
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("Se requiere secreto");
        if (signable.keySet().stream().anyMatch(Set.of("file","api_key","signature","cloud_name","resource_type")::contains))
            throw new IllegalArgumentException("Parámetro no firmable");
        var sorted = new TreeMap<>(signable);
        String payload = sorted.entrySet().stream().map(e -> e.getKey()+"="+e.getValue())
            .collect(Collectors.joining("&")) + secret;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 no disponible"); }
    }
}
