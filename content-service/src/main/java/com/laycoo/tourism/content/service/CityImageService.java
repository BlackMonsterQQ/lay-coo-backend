package com.laycoo.tourism.content.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.content.entity.City;
import com.laycoo.tourism.content.mapper.CityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * 城市地标影像代理：优先使用本地缓存，其次从 Wikipedia 获取并缓存。
 * 浏览器只访问本地接口，避免跨域、代理和图片防盗链问题。
 */
@Slf4j
@Service
public class CityImageService {

    private static final Pattern BAD_IMAGE = Pattern.compile(
            "(subdivisions|location[_-]?map|locator|map_of|flag|coat[_-]?of[_-]?arms|logo|seal|\\.svg)",
            Pattern.CASE_INSENSITIVE);

    private final CityMapper cityMapper;
    private final ObjectMapper objectMapper;
    private final Path cacheDir;
    private final HttpClient directClient;
    private final HttpClient proxyClient;

    public CityImageService(CityMapper cityMapper,
                            ObjectMapper objectMapper,
                            @Value("${city.image.proxy:}") String proxy,
                            @Value("${city.image.cache-dir:./data/city-images}") String cacheDir) {
        this.cityMapper = cityMapper;
        this.objectMapper = objectMapper;
        this.cacheDir = Path.of(cacheDir);
        this.directClient = buildClient(null);
        this.proxyClient = buildClient(proxy);
    }

    public ImageResult load(String code) {
        City city = cityMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<City>()
                .eq(City::getCode, code)
                .last("limit 1"));
        if (city == null) {
            return null;
        }
        try {
            Files.createDirectories(cacheDir);
            Path cached = cacheDir.resolve(code + ".img");
            Path typeFile = cacheDir.resolve(code + ".type");
            if (Files.exists(cached) && Files.size(cached) > 1024) {
                String type = Files.exists(typeFile) ? Files.readString(typeFile).trim() : "image/jpeg";
                return new ImageResult(Files.readAllBytes(cached), type);
            }
        } catch (Exception e) {
            log.warn("读取城市图片缓存失败 code={}: {}", code, e.getMessage());
        }

        String imageUrl = findImageUrl(city);
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        ImageResult image = download(imageUrl);
        if (image == null) {
            return null;
        }
        try {
            Files.createDirectories(cacheDir);
            Files.write(cacheDir.resolve(code + ".img"), image.bytes());
            Files.writeString(cacheDir.resolve(code + ".type"), image.contentType());
        } catch (Exception e) {
            log.warn("写入城市图片缓存失败 code={}: {}", code, e.getMessage());
        }
        return image;
    }

    private String findImageUrl(City city) {
        String url = pageImage(city.getName());
        if (!StringUtils.hasText(url) && !city.getName().endsWith("市")) {
            url = pageImage(city.getName() + "市");
        }
        if (!StringUtils.hasText(url)) {
            url = summaryImage(city.getName());
        }
        return url;
    }

    private String pageImage(String name) {
        String url = "https://zh.wikipedia.org/w/api.php?action=query&format=json&prop=pageimages"
                + "&pithumbsize=1800&titles=" + encode(name);
        String body = getString(url);
        if (!StringUtils.hasText(body)) {
            return "";
        }
        try {
            JsonNode pages = objectMapper.readTree(body).path("query").path("pages");
            Iterator<JsonNode> iterator = pages.elements();
            while (iterator.hasNext()) {
                String source = iterator.next().path("thumbnail").path("source").asText("");
                if (isUsable(source)) {
                    return source;
                }
            }
        } catch (Exception e) {
            log.warn("解析 Wikipedia pageimages 失败 name={}: {}", name, e.getMessage());
        }
        return "";
    }

    private String summaryImage(String name) {
        String body = getString("https://zh.wikipedia.org/api/rest_v1/page/summary/" + encode(name));
        if (!StringUtils.hasText(body)) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            String source = node.path("originalimage").path("source").asText("");
            if (!isUsable(source)) {
                source = node.path("thumbnail").path("source").asText("");
            }
            return isUsable(source) ? source : "";
        } catch (Exception e) {
            log.warn("解析 Wikipedia summary 失败 name={}: {}", name, e.getMessage());
        }
        return "";
    }

    private String getString(String url) {
        HttpResponse<String> response = send(url, HttpResponse.BodyHandlers.ofString(), false);
        if (response == null || response.statusCode() != 200) {
            return "";
        }
        return response.body();
    }

    private ImageResult download(String url) {
        HttpResponse<byte[]> response = send(url, HttpResponse.BodyHandlers.ofByteArray(), true);
        if (response == null || response.statusCode() != 200 || response.body() == null
                || response.body().length < 1024) {
            return null;
        }
        String contentType = response.headers().firstValue("content-type").orElse("image/jpeg");
        return new ImageResult(response.body(), contentType);
    }

    private <T> HttpResponse<T> send(String url, HttpResponse.BodyHandler<T> handler, boolean image) {
        HttpResponse<T> response = proxyClient == null ? null : execute(proxyClient, url, handler);
        if (response != null && response.statusCode() == 200) {
            return response;
        }
        HttpResponse<T> direct = execute(directClient, url, handler);
        if (direct != null && direct.statusCode() == 200) {
            return direct;
        }
        log.warn("城市图片请求失败 url={} image={}", url, image);
        return direct == null ? response : direct;
    }

    private <T> HttpResponse<T> execute(HttpClient client, String url, HttpResponse.BodyHandler<T> handler) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(25))
                    .header("User-Agent", "LayCoo/1.0 (local cultural tourism demo)")
                    .header("Accept", "application/json,image/*")
                    .GET()
                    .build();
            return client.send(request, handler);
        } catch (Exception e) {
            log.debug("城市图片请求异常 {}: {}", url, e.getMessage());
            return null;
        }
    }

    private HttpClient buildClient(String proxy) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (StringUtils.hasText(proxy)) {
            try {
                URI uri = URI.create(proxy);
                builder.proxy(ProxySelector.of(new InetSocketAddress(uri.getHost(), uri.getPort())));
            } catch (Exception e) {
                log.warn("城市图片代理配置无效: {}", proxy);
            }
        }
        return builder.build();
    }

    private boolean isUsable(String url) {
        return StringUtils.hasText(url) && !BAD_IMAGE.matcher(url).find();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record ImageResult(byte[] bytes, String contentType) {
    }
}
