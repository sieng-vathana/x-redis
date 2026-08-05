package com.x.redis.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XRedisAutoConfigurationTest {

    @Test
    void roundTripsFinalRecordValuesWithTypeMetadata() {
        var serializer = XRedisAutoConfiguration.jsonSerializer();
        var value = new FinalPage<>(List.of(new FinalItem(1L, "Main Store")), 1L);

        Object restored = serializer.deserialize(serializer.serialize(value));

        assertEquals(value, restored);
    }

    private record FinalPage<T>(List<T> content, long totalElements) {
    }

    private record FinalItem(Long id, String name) {
    }
}
