package noesis.core.retrieval;

/**
 * Maps free text to a fixed-length dense vector.
 *
 * <p>The framework ships a deterministic {@link HashEmbedding} so retrieval is
 * fully reproducible offline; production deployments can drop in a real
 * embedding model (for example a hosted {@code text-embedding-3-small}
 * endpoint) by implementing this single method.</p>
 */
public interface Embedding {
    float[] embed(String text);
}
