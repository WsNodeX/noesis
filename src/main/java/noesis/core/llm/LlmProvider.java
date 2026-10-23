package noesis.core.llm;

/**
 * Abstraction over a chat-completion backend.
 *
 * <p>Production code plugs in {@link OpenAICompatibleClient}; tests and offline
 * demos plug in a scripted stub. Decoupling the agent and retrieval layers from
 * any concrete endpoint keeps the whole framework dependency-free and easy to
 * test.</p>
 */
public interface LlmProvider {
    /** Run a blocking chat completion and return the full response. */
    ChatResponse chat(ChatRequest request);

    /** Stream a chat completion, pushing tokens to the supplied handler. */
    void stream(ChatRequest request, StreamHandler handler);
}
