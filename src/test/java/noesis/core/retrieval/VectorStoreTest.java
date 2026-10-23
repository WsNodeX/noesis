package noesis.core.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class VectorStoreTest {

    @Test
    void returnsExactTopK() {
        HashEmbedding e = new HashEmbedding(256, false);
        VectorStore store = new VectorStore();
        store.add(new Document("a", "nearest neighbour search in vector databases"), e);
        store.add(new Document("b", "how to bake sourdough bread at home"), e);
        store.add(new Document("c", "vector index approximate search with HNSW"), e);
        store.add(new Document("d", "training a neural network from scratch"), e);

        List<ScoredDocument> hits = store.search(e.embed("vector search nearest neighbour"), 2);
        assertEquals(2, hits.size());
        assertEquals("a", hits.get(0).document().id());
        assertTrue(hits.get(0).score() > hits.get(1).score());
    }

    @Test
    void identicalTextScoresOne() {
        HashEmbedding e = new HashEmbedding(128, false);
        VectorStore store = new VectorStore();
        store.add(new Document("x", "identical content here"), e);
        List<ScoredDocument> hits = store.search(e.embed("identical content here"), 1);
        assertEquals(1.0, hits.get(0).score(), 1e-6);
    }

    @Test
    void respectsRequestedKWhenSmallerThanSize() {
        HashEmbedding e = new HashEmbedding(64, false);
        VectorStore store = new VectorStore();
        for (int i = 0; i < 3; i++) {
            store.add(new Document("d" + i, "doc number " + i), e);
        }
        assertEquals(2, store.search(e.embed("doc number one"), 2).size());
        assertEquals(3, store.search(e.embed("doc number one"), 10).size());
    }
}
