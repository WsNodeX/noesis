package noesis.core.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashEmbeddingTest {

    @Test
    void isDeterministic() {
        HashEmbedding e = new HashEmbedding();
        float[] a = e.embed("the quick brown fox");
        float[] b = e.embed("the quick brown fox");
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            assertEquals(a[i], b[i], 1e-6, "vector must be stable across calls");
        }
    }

    @Test
    void isL2Normalized() {
        HashEmbedding e = new HashEmbedding(128, false);
        float[] v = e.embed("retrieval augmented generation over a corpus");
        double norm = 0.0;
        for (float x : v) norm += (double) x * x;
        assertEquals(1.0, Math.sqrt(norm), 1e-5);
    }

    @Test
    void distinguishesDifferentText() {
        HashEmbedding e = new HashEmbedding(256, false);
        float[] x = e.embed("machine learning model training pipeline");
        float[] y = e.embed("banana smoothie recipe with ice cream");
        double sim = VectorStore.cosine(x, y);
        assertTrue(sim < 0.9, "unrelated texts should have low cosine similarity");
    }

    @Test
    void similarTextScoresHigherThanRandom() {
        HashEmbedding e = new HashEmbedding(256, false);
        float[] q = e.embed("vector database nearest neighbour search");
        float[] near = e.embed("nearest neighbour search in a vector database");
        float[] far = e.embed("the weather today is sunny and warm outside");
        assertTrue(VectorStore.cosine(q, near) > VectorStore.cosine(q, far));
    }
}
