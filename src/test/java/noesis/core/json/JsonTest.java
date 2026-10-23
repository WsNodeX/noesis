package noesis.core.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesFlatObject() {
        Map<String, Object> o = Json.parseObject("{\"name\":\"noesis\",\"v\":2,\"ok\":true,\"n\":null}");
        assertEquals("noesis", Json.getString(o, "name"));
        assertEquals(2.0, Json.getDouble(o, "v"));
        assertEquals(true, Json.getBool(o, "ok", false));
        assertTrue(o.containsKey("n"));
    }

    @Test
    void parsesNestedAndArrays() {
        Map<String, Object> o = Json.parseObject("{\"a\":{\"b\":[1,2,3]},\"c\":[{\"x\":9}]}");
        Map<String, Object> a = Json.getObject(o, "a");
        List<Object> b = Json.getArray(a, "b");
        assertEquals(3, b.size());
        assertEquals(3.0, ((Number) b.get(2)).doubleValue());
        List<Object> c = Json.getArray(o, "c");
        Map<String, Object> inner = (Map<String, Object>) c.get(0);
        assertEquals(9.0, Json.getDouble(inner, "x"));
    }

    @Test
    void handlesEscapes() {
        Map<String, Object> o = Json.parseObject("{\"s\":\"line1\\nline2\\ttab\\\"q\\u0041\"}");
        assertEquals("line1\nline2\ttab\"qA", Json.getString(o, "s"));
    }

    @Test
    void roundTripsThroughWriter() {
        String json = Json.write(Json.object()
                .put("name", "noesis")
                .put("count", 3)
                .put("enabled", true)
                .put("items", Json.array().add("a").add("b").build())
                .build());
        String written = json;
        Map<String, Object> back = Json.parseObject(written);
        assertEquals("noesis", Json.getString(back, "name"));
        assertEquals(3.0, Json.getDouble(back, "count"));
        assertEquals(true, Json.getBool(back, "enabled", false));
        assertEquals(2, Json.getArray(back, "items").size());
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(Json.JsonException.class, () -> Json.parseObject("{bad"));
        assertThrows(Json.JsonException.class, () -> Json.parseObject("{\"a\":}"));
    }

    @Test
    void writerEscapesSpecialCharacters() {
        String out = Json.write(Json.object().put("k", "a\"b\\c").build());
        assertNotNull(out);
        Map<String, Object> back = Json.parseObject(out);
        assertEquals("a\"b\\c", Json.getString(back, "k"));
    }
}
