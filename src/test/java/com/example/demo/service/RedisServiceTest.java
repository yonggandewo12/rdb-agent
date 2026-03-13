package com.example.demo.service;

import com.example.demo.service.impl.RedisServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = RedisServiceImpl.class)
public class RedisServiceTest {
    
    @MockBean
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RedisService redisService;
    
    @Test
    public void testGet() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("testKey")).thenReturn("testValue");
        
        String result = redisService.get("testKey");
        assertEquals("testValue", result);
        verify(valueOps, times(1)).get("testKey");
    }
    
    @Test
    public void testSet() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        
        redisService.set("testKey", "testValue");
        verify(valueOps, times(1)).set("testKey", "testValue");
    }
    
    @Test
    public void testSetWithExpire() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        
        redisService.set("testKey", "testValue", 60);
        verify(valueOps, times(1)).set(eq("testKey"), eq("testValue"), eq(60L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    public void testDelete() {
        when(redisTemplate.delete("testKey")).thenReturn(true);
        
        Boolean result = redisService.delete("testKey");
        assertTrue(result);
        verify(redisTemplate, times(1)).delete("testKey");
    }
    
    @Test
    public void testExists() {
        when(redisTemplate.hasKey("testKey")).thenReturn(true);
        
        Boolean result = redisService.exists("testKey");
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey("testKey");
    }
    
    @Test
    public void testIncrement() {
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment("counter", 1)).thenReturn(2L);
        
        Long result = redisService.increment("counter", 1);
        assertEquals(2L, result);
        verify(valueOps, times(1)).increment("counter", 1);
    }
}
