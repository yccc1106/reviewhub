package com.hmdp.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisWorkerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisWorker redisWorker;

    /** 基准时间戳对应的秒值: 2026-01-01 00:00:00 UTC */
    private static final long BEGIN_TIMESTAMP = 1767225600L;
    private static final int COUNT_BITS = 32;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("正常生成ID — 验证ID非空且为正数")
    void testNextId_ReturnsPositiveLong() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        Long id = redisWorker.nextId("order");
        assertNotNull(id);
        assertTrue(id > 0);
    }

    @Test
    @DisplayName("Redis key格式正确 — 日期部分精确到天")
    void testNextId_UsesCorrectRedisKeyFormat() {
        String keyPrefix = "order";
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String expectedKey = "icr" + keyPrefix + ":" + today;

        when(valueOperations.increment(expectedKey)).thenReturn(1L);

        Long id = redisWorker.nextId(keyPrefix);
        // increment返回1，ID的最低位应该是1
        assertEquals(1L, id & ((1L << COUNT_BITS) - 1));
    }

    @Test
    @DisplayName("不同keyPrefix对应的序列号独立")
    void testNextId_DifferentPrefixesIndependentCounters() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));

        when(valueOperations.increment("icrorder:" + today)).thenReturn(5L);
        when(valueOperations.increment("icruser:" + today)).thenReturn(10L);

        Long orderId = redisWorker.nextId("order");
        Long userId = redisWorker.nextId("user");

        long orderSeq = orderId & ((1L << COUNT_BITS) - 1);
        long userSeq = userId & ((1L << COUNT_BITS) - 1);

        assertEquals(5L, orderSeq);
        assertEquals(10L, userSeq);
    }

    @Test
    @DisplayName("高32位为时间戳差值")
    void testNextId_HighBitsAreTimestampDelta() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        long before = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;
        Long id = redisWorker.nextId("test");
        long after = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - BEGIN_TIMESTAMP;

        long timeSnapFromId = id >> COUNT_BITS;
        assertTrue(timeSnapFromId >= before);
        assertTrue(timeSnapFromId <= after);
    }

    @Test
    @DisplayName("低32位为Redis自增序列号")
    void testNextId_LowBitsAreSequenceNumber() {
        long expectedSeq = 42L;
        when(valueOperations.increment(anyString())).thenReturn(expectedSeq);

        Long id = redisWorker.nextId("test");
        long sequenceNumber = id & ((1L << COUNT_BITS) - 1);

        assertEquals(expectedSeq, sequenceNumber);
    }

    @Test
    @DisplayName("Redis key包含当天日期")
    void testNextId_KeyContainsTodayDate() {
        String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String expectedKey = "icrtest:" + today;

        when(valueOperations.increment(expectedKey)).thenReturn(88L);

        Long id = redisWorker.nextId("test");
        long sequenceNumber = id & ((1L << COUNT_BITS) - 1);
        assertEquals(88L, sequenceNumber);
    }

    @Test
    @DisplayName("递增调用 — 序列号连续递增")
    void testNextId_IncrementingSequence() {
        when(valueOperations.increment(anyString()))
                .thenReturn(1L)
                .thenReturn(2L)
                .thenReturn(3L);

        Long id1 = redisWorker.nextId("test");
        Long id2 = redisWorker.nextId("test");
        Long id3 = redisWorker.nextId("test");

        long seq1 = id1 & ((1L << COUNT_BITS) - 1);
        long seq2 = id2 & ((1L << COUNT_BITS) - 1);
        long seq3 = id3 & ((1L << COUNT_BITS) - 1);

        assertEquals(1L, seq1);
        assertEquals(2L, seq2);
        assertEquals(3L, seq3);
    }

    @Test
    @DisplayName("生成的ID单调递增")
    void testNextId_MonotonicallyIncreasing() {
        // 模拟在同一秒内自增
        when(valueOperations.increment(anyString()))
                .thenReturn(1L)
                .thenReturn(2L);

        Long id1 = redisWorker.nextId("test");
        Long id2 = redisWorker.nextId("test");

        assertTrue(id2 > id1);
    }
}
