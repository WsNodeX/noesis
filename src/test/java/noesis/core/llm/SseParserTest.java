package noesis.core.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class SseParserTest {

    @Test
    void extractsDataPayload() {
        assertEquals("hello", SseParser.dataOf("data: hello"));
        assertEquals("hello", SseParser.dataOf("data:hello"));
        assertEquals("{\"a\":1}", SseParser.dataOf("data: {\"a\":1}"));
    }

    @Test
    void ignoresNonDataAndComments() {
        assertNull(SseParser.dataOf(""));
        assertNull(SseParser.dataOf("   "));
        assertNull(SseParser.dataOf(": keep-alive comment"));
        assertNull(SseParser.dataOf("event: ping"));
    }

    @Test
    void detectsDoneMarker() {
        assertTrue(SseParser.isDone("[DONE]"));
        assertFalse(SseParser.isDone("{\"choices\":[]}"));
    }

    @Test
    void consumesMultiLineStream() throws IOException {
        String stream = """
                data: {"choices":[{"delta":{"content":"He"}}]}

                data: {"choices":[{"delta":{"content":"llo"}}]}

                data: [DONE]

                """;
        List<String> payloads = new ArrayList<>();
        SseParser.consume(new StringReader(stream), payloads::add);
        assertEquals(List.of(
                "{\"choices\":[{\"delta\":{\"content\":\"He\"}}]}",
                "{\"choices\":[{\"delta\":{\"content\":\"llo\"}}]}"), payloads);
    }
}
