package org.example.carpet.controller;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@RestController
@RequestMapping("/media")
public class ImageProxyController {

    private final RestTemplate rest;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // 安全起见，只允许代理这些域名（按需增减）
    private static final Set<String> ALLOW_HOSTS = Set.of(
            "www.yancarpet.com", "yancarpet.com"
    );

    public ImageProxyController(RestTemplate rest) {
        this.rest = rest;
    }

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) throws URISyntaxException {
        URI target = new URI(url);

        // 1) 简单安全校验：只允许白名单域名，避免把你的服务当“开放代理”
        String host = target.getHost() == null ? "" : target.getHost().toLowerCase();
        if (!ALLOW_HOSTS.contains(host)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(("Blocked host: " + host).getBytes(StandardCharsets.UTF_8));
        }

        // 2) 构造上游请求头（有些站要求 UA/Referer）
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (YanCarpet Proxy)");
        headers.set("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
        // 如果目标站需要引用来源，可以放开这一行（很多国内站要）：
        headers.set("Referer", "https://www.yancarpet.com/");

        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> resp = rest.exchange(target, HttpMethod.GET, req, byte[].class);

            // 3) 传递内容类型（缺省设为 JPEG），并设置缓存头（可选）
            MediaType ct = resp.getHeaders().getContentType();
            if (ct == null) ct = MediaType.IMAGE_JPEG;

            HttpHeaders out = new HttpHeaders();
            out.setContentType(ct);
            // 允许前端缓存 1 天（按需调整/去掉）
            out.setCacheControl(CacheControl.maxAge(86400, java.util.concurrent.TimeUnit.SECONDS).cachePublic());

            return new ResponseEntity<>(resp.getBody(), out, HttpStatus.OK);
        } catch (RestClientResponseException e) {
            // 上游返回非 2xx
            String msg = "Upstream error " + e.getRawStatusCode() + ": " + e.getStatusText();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            String msg = "Proxy failed: " + ex.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(msg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
