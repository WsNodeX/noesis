package noesis.examples;

import java.util.ArrayDeque;
import java.util.Deque;

import noesis.core.llm.ChatRequest;
import noesis.core.llm.ChatResponse;
import noesis.core.llm.LlmProvider;
import noesis.core.llm.StreamHandler;

/**
 * A scripted {@link LlmProvider} that returns pre-canned responses in order.
 *
 * <p>It exists so the agent and RAG layers can be demonstrated and tested with
 * zero network access: tests queue the exact model replies (including tool-call
 * requests) they need, and the offline demo uses it to simulate a real
 * conversation. Production code would use {@code OpenAICompatibleClient}
 * instead.</p>
 */
public final class StubProvider implements LlmProvider {
    private final Deque<ChatResponse> script = new ArrayDeque<>();
    private ChatResponse fallback;

    /** Queue the next response to be returned. */
    public StubProvider then(ChatResponse response) {
        script.addLast(response);
        return this;
    }

    /** Response used once the script is exhausted. */
    public StubProvider fallback(ChatResponse response) {
        this.fallback = response;
        return this;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        if (!script.isEmpty()) {
            return script.pollFirst();
        }
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("StubProvider has no response configured");
    }

    @Override
    public void stream(ChatRequest request, StreamHandler handler) {
        ChatResponse response = chat(request);
        var message = response.firstMessage();
        if (message != null && message.content() != null) {
            handler.onToken(message.content());
        }
        handler.onComplete();
    }
}
