package com.bgssai.shortai.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local disk fallback when OBS secrets are missing (local / early env).
 */
public class LocalFileObjectStorage implements ObjectStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileObjectStorage.class);

    private final Path root;
    private final String publicBaseUrl;
    private final String signSecret;

    public LocalFileObjectStorage(String localRoot, String publicBaseUrl, String signSecret) {
        this.root = Path.of(localRoot == null || localRoot.isBlank() ? "./data/obs-local" : localRoot)
                .toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/$", "");
        this.signSecret = signSecret == null || signSecret.isBlank() ? "bgssai-local-object-sign" : signSecret;
        try {
            Files.createDirectories(this.root);
        } catch (IOException ex) {
            throw new IllegalStateException("cannot create local object root: " + this.root, ex);
        }
    }

    @Override
    public String put(String objectKey, byte[] content, String mimeType) {
        Path target = resolve(objectKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content == null ? new byte[0] : content);
            if (mimeType != null && !mimeType.isBlank()) {
                Files.writeString(Path.of(target.toString() + ".mime"), mimeType.trim(), StandardCharsets.UTF_8);
            }
            return objectKey;
        } catch (IOException ex) {
            throw new IllegalStateException("local put failed: " + objectKey, ex);
        }
    }

    @Override
    public byte[] get(String objectKey) {
        try {
            Path target = resolve(objectKey);
            if (!Files.isRegularFile(target)) {
                return null;
            }
            return Files.readAllBytes(target);
        } catch (IOException ex) {
            throw new IllegalStateException("local get failed: " + objectKey, ex);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Files.deleteIfExists(resolve(objectKey));
            Files.deleteIfExists(Path.of(resolve(objectKey).toString() + ".mime"));
        } catch (IOException ex) {
            log.warn("local delete failed key={}", objectKey, ex);
        }
    }

    @Override
    public PresignResult presignGet(String objectKey, int expireSeconds) {
        long exp = Instant.now().getEpochSecond() + Math.max(60, expireSeconds);
        String sig = sign(objectKey, exp);
        String base = publicBaseUrl.isEmpty() ? "" : publicBaseUrl;
        String url = base + "/bgssai/user/local-objects?key=" + urlEncode(objectKey)
                + "&exp=" + exp + "&sig=" + sig;
        return new PresignResult(url, Instant.ofEpochSecond(exp));
    }

    public boolean verify(String objectKey, long exp, String sig) {
        if (objectKey == null || sig == null) {
            return false;
        }
        if (Instant.now().getEpochSecond() > exp) {
            return false;
        }
        return sign(objectKey, exp).equalsIgnoreCase(sig);
    }

    public String mimeOf(String objectKey) {
        try {
            Path mime = Path.of(resolve(objectKey).toString() + ".mime");
            if (Files.isRegularFile(mime)) {
                return Files.readString(mime, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ignored) {
            // fall through
        }
        return "application/octet-stream";
    }

    private Path resolve(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey required");
        }
        Path resolved = root.resolve(objectKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("invalid objectKey");
        }
        return resolved;
    }

    private String sign(String objectKey, long exp) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((objectKey + "|" + exp + "|" + signSecret).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception ex) {
            throw new IllegalStateException("sign failed", ex);
        }
    }

    private static String urlEncode(String raw) {
        return java.net.URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }
}
