package com.bgssai.shortai.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves LocalFileObjectStorage objects via short-lived signed query params (no JWT).
 */
@RestController
public class LocalObjectDownloadController {

    @Autowired
    private ObjectStorage objectStorage;

    @GetMapping("/bgssai/user/local-objects")
    public ResponseEntity<byte[]> handle(@RequestParam("key") String key,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig) {
        if (!(objectStorage instanceof LocalFileObjectStorage local)) {
            return ResponseEntity.notFound().build();
        }
        if (!local.verify(key, exp, sig)) {
            return ResponseEntity.status(403).build();
        }
        byte[] body = local.get(key);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        String mime = local.mimeOf(key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=60")
                .contentType(MediaType.parseMediaType(mime))
                .body(body);
    }
}
