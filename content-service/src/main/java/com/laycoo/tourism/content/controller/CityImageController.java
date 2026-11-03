package com.laycoo.tourism.content.controller;

import com.laycoo.tourism.content.service.CityImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 城市地标图片代理，前端直接以背景图 URL 使用。
 */
@RestController
@RequestMapping("/api/v1/city")
@RequiredArgsConstructor
public class CityImageController {

    private final CityImageService cityImageService;

    @GetMapping("/{code}/image")
    public ResponseEntity<byte[]> image(@PathVariable String code) {
        CityImageService.ImageResult image = cityImageService.load(code);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(image.bytes());
    }
}
