package com.jojo.redis.controller;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import redis.clients.jedis.Jedis;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class SessionControllerTest {

    private Jedis jedis;
    private TestRestTemplate restTestTemplate;
    private TestRestTemplate testRestTemplateWithAuth;
    private String testUrl = "http://localhost:8080/";

    @BeforeEach
    public void clearRedisData() {
        restTestTemplate = new TestRestTemplate();
        testRestTemplateWithAuth = new TestRestTemplate("admin", "password", null);

        jedis = new Jedis("localhost", 6379);
        jedis.flushAll();
    }

    @Test
    public void testRedisIsEmpty() {
        Set<String> result = jedis.keys("*");
        assertEquals(0, result.size());
    }

    @Test
    public void testUnauthenticatedCantAccess() {
        ResponseEntity<String> result = restTestTemplate.getForEntity(testUrl, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    public void testRedisControlsSession() {
        ResponseEntity<String> result = testRestTemplateWithAuth.getForEntity(testUrl, String.class);
        assertEquals("hello admin", result.getBody()); //login worked

        Set<String> redisResult = jedis.keys("*");
        assertTrue(redisResult.size() > 0); //redis is populated with session data

        String sessionCookie = result.getHeaders().get("Set-Cookie").get(0).split(";")[0];
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sessionCookie);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        result = restTestTemplate.exchange(testUrl, HttpMethod.GET, httpEntity, String.class);
        assertEquals("hello admin", result.getBody()); //access with session works worked

        jedis.flushAll(); //clear all keys in redis

        result = restTestTemplate.exchange(testUrl, HttpMethod.GET, httpEntity, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
        //access denied after sessions are removed in redis
    }
}
