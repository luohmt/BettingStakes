package com.betting.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightJsonParserTest {

    @Test
    void testJson() {
        String json1 = "{\"stake\":4500}";
        String json2 = "{\"stake\": \"4500\", \"active\": true}";
        LightJsonParser p1 = new LightJsonParser(json1);
        LightJsonParser p2 = new LightJsonParser(json2);

        assertEquals(4500, p1.getInt("stake"));
        assertEquals(4500, p2.getInt("stake"));
        assertTrue(p2.getBoolean("active"));
    }
}