package noesis.core.rag;

import java.util.List;

import noesis.core.json.Json;
import noesis.core.llm.ChatMessage;
import noesis.core.llm.ChatRequest;
import noesis.core.llm.ChatResponse;
import noesis.core.llm.LlmProvider;
import noesis.core.retrieval.Document;
import noesis.core.retrieval.Embedding;
import noesis.core.retrieval.ScoredDocument;
import noesis.core.retrieval.TextChunker;
import noesis.core.retrieval.VectorStore;

/**
 * Retrieval-augmented generation over a private corpus.
 *
 * <p>Documents are chunked, embedded and indexed once (via {@link #ingest});
 * every {@link #answer} call embeds the question, retrieves the most relevant
 * passages, assembles a grounded prompt and asks the model to answer strictly
 * from the supplied context. Because retrieval and generation are separate
 * seams, the same pipeline works offline (with {@link noesis.core.retrieval.HashEmbedding}
 * and a scripted provider) or in production (with a hosted embedding model and
 * a real LLM endpoint).</p>
 */
public final class RagPipeline {
    private final Embedding embedding;
    private final LlmProvider provider;
    private final String model;
    private final int topK;
    private final VectorStore store = new VectorStore();
    private final TextChunker chunker = new TextChunker();
    private int autoId = 0;

    public RagPipeline(Embedding embedding, LlmProvider provider, String model) {
        this(embedding, provider, model, 4);
    }

    public RagPipeline(Embedding embedding, LlmProvider provider, String model, int topK) {
        this.embedding = embedding;
        this.provider = provider;
        this.model = model;
        this.topK = topK;
    }

    /** Ingest a document, chunking it into retrievable passages. */
    public void ingest(String docId, String text) {
        for (String chunk : chunker.chunk(text)) {
            store.add(new Document(docId, chunk), embedding);
        }
    }

    /** Ingest a document under an auto-generated identifier. */
    public void ingestText(String text) {
        ingest("doc-" + (++autoId), text);
    }

    public int indexedChunks() {
        return store.size();
    }

    /** Answer a question grounded in the indexed corpus. */
    public Answer answer(String question) {
        List<ScoredDocument> hits = store.search(embedding.embed(question), topK);
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            context.append("[").append(i + 1).append("] ")
                   .append(hits.get(i).document().text())
                   .append("\n");
        }
        String prompt = buildPrompt(question, context.toString());
        ChatResponse response = provider.chat(
                ChatRequest.builder(model).message(ChatMessage.user(prompt)).build());
        String content = response.firstMessage() == null ? "" : response.firstMessage().content();
        return new Answer(content, hits);
    }

    private String buildPrompt(String question, String context) {
        return "You are a precise assistant. Answer the question using ONLY the "
                + "context below. If the context does not contain the answer, say "
                + "you do not know. Cite the passage numbers you used.\n\n"
                + "CONTEXT:\n" + context + "\nQUESTION: " + question;
    }

    /** Quick JSON snapshot of the retrieved sources for logging/debugging. */
    public String describeSources(List<ScoredDocument> hits) {
        var arr = Json.array();
        for (ScoredDocument h : hits) {
            arr.add(Json.object()
                    .put("id", h.document().id())
                    .put("score", Math.round(h.score() * 10000.0) / 10000.0)
                    .put("text", h.document().text())
                    .build());
        }
        return Json.write(arr.build());
    }
}
