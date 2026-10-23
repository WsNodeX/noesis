package noesis;

/**
 * Noesis &mdash; a zero-dependency Java framework for building LLM-powered
 * agents with tool-calling, streaming chat and semantic (vector) memory.
 *
 * <p>It bundles four cooperating layers behind a tiny public surface:</p>
 * <ul>
 *   <li>{@code noesis.core.llm} &mdash; chat messages, requests/responses and an
 *       OpenAI-compatible streaming client;</li>
 *   <li>{@code noesis.core.tools} &mdash; a registry and executor for
 *       model-invokable tools (function calling);</li>
 *   <li>{@code noesis.core.retrieval} &mdash; embeddings, chunking, an in-memory
 *       vector store and semantic search;</li>
 *   <li>{@code noesis.core.agent} / {@code noesis.core.rag} &mdash; a ReAct
 *       agent loop and a retrieval-augmented generation pipeline.</li>
 * </ul>
 */
public final class Noesis {
    /** The framework version. */
    public static final String VERSION = "1.0.0";

    private Noesis() {}

    /** Print a short banner describing the framework. */
    public static String banner() {
        return "Noesis " + VERSION + " — agent + semantic-memory toolkit for the JVM";
    }
}
