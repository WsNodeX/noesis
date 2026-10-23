package noesis.core.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import noesis.core.llm.ChatMessage;
import noesis.core.llm.ChatResponse;
import noesis.core.retrieval.HashEmbedding;
import noesis.examples.StubProvider;

import org.junit.jupiter.api.Test;

class RagPipelineTest {

    @Test
    void answersFromIndexedCorpus() {
        StubProvider provider = new StubProvider().fallback(
                ChatResponse.builder().choices(List.of(ChatMessage.assistant(
                        "Aurora supports streaming and tool calling."))).build());

        RagPipeline rag = new RagPipeline(new HashEmbedding(), provider, "stub");
        rag.ingest("spec", "Aurora is a lightweight inference server written in Rust.");
        rag.ingest("spec", "Aurora supports streaming responses and function calling for tools.");
        rag.ingest("spec", "Aurora scales horizontally with a built-in tokenizer cache.");

        Answer answer = rag.answer("Does Aurora support streaming and tool calling?");
        assertEquals("Aurora supports streaming and tool calling.", answer.content());
        assertFalse(answer.sources().isEmpty());
        // The most relevant chunk should be the one mentioning streaming/tools.
        assertTrue(answer.sources().get(0).document().text().contains("streaming"));
    }

    @Test
    void retrievalRanksRelevantPassageFirst() {
        RagPipeline rag = new RagPipeline(new HashEmbedding(),
                new StubProvider().fallback(
                        ChatResponse.builder().choices(List.of(ChatMessage.assistant("see sources"))).build()),
                "stub");
        rag.ingest("d1", "Irrelevant text about cooking pasta with tomato sauce.");
        rag.ingest("d2", "Noesis provides semantic search over dense vector embeddings.");
        rag.ingest("d3", "The weather forecast predicts rain for the weekend.");

        Answer answer = rag.answer("How does semantic vector search work?");
        assertEquals("d2", answer.sources().get(0).document().id());
    }
}
