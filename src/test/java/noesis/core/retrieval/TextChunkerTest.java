package noesis.core.retrieval;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TextChunkerTest {

    @Test
    void splitsLongTextIntoMultipleChunks() {
        TextChunker chunker = new TextChunker(80, 20);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("Sentence ").append(i).append(" about retrieval and embeddings. ");
        }
        List<String> chunks = chunker.chunk(sb.toString());
        assertTrue(chunks.size() > 1, "long text should be split into several chunks");
        for (String c : chunks) {
            assertFalse(c.isBlank());
        }
    }

    @Test
    void shortTextStaysSingleChunk() {
        TextChunker chunker = new TextChunker(400, 80);
        List<String> chunks = chunker.chunk("A single short sentence.");
        assertTrue(chunks.size() == 1);
    }

    @Test
    void preservesAllTokensAcrossChunks() {
        TextChunker chunker = new TextChunker(60, 15);
        String text = "Alpha beta gamma. Delta epsilon zeta. Eta theta iota. Kappa lambda mu.";
        List<String> chunks = chunker.chunk(text);
        String joined = String.join(" ", chunks).replaceAll("\\s+", " ");
        for (String token : new String[]{"Alpha", "gamma", "Delta", "iota", "Kappa", "mu"}) {
            assertTrue(joined.contains(token), "token '" + token + "' must survive chunking");
        }
    }
}
