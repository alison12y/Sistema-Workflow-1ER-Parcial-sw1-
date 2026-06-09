package com.workflow.politicas.service;

import com.workflow.politicas.storage.OnlyOfficeProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;

@Service
public class OnlyOfficeAccessTokenService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final OnlyOfficeProperties properties;

    public OnlyOfficeAccessTokenService(OnlyOfficeProperties properties) {
        this.properties = properties;
    }

    public String createFileAccessToken(String documentId, String username) {
        return createToken(documentId, username, "FILE", DEFAULT_TTL);
    }

    public String createCallbackToken(String documentId, String username) {
        return createToken(documentId, username, "CALLBACK", DEFAULT_TTL);
    }

    public void validateToken(String token, String documentId, String purpose) {
        TokenParts parts = parseToken(token);
        if (!documentId.equals(parts.documentId())) {
            throw new IllegalArgumentException("Token no corresponde al documento");
        }
        if (!purpose.equalsIgnoreCase(parts.purpose())) {
            throw new IllegalArgumentException("Token inválido para esta operación");
        }
        if (Instant.now().getEpochSecond() > parts.expiresAtEpoch()) {
            throw new IllegalArgumentException("Token expirado");
        }
    }

    private String createToken(String documentId, String username, String purpose, Duration ttl) {
        long exp = Instant.now().plus(ttl).getEpochSecond();
        String payload = documentId + "|" + username + "|" + purpose.toUpperCase(Locale.ROOT) + "|" + exp;
        String signature = sign(payload);
        String raw = payload + "|" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private TokenParts parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token requerido");
        }
        String decoded;
        try {
            decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Token malformado");
        }
        String[] parts = decoded.split("\\|");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Token malformado");
        }
        String expectedSig = sign(parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3]);
        if (!expectedSig.equals(parts[4])) {
            throw new IllegalArgumentException("Token inválido");
        }
        return new TokenParts(parts[0], parts[1], parts[2], Long.parseLong(parts[3]));
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar token OnlyOffice", ex);
        }
    }

    private record TokenParts(String documentId, String username, String purpose, long expiresAtEpoch) {
    }
}
