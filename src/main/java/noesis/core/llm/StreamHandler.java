package noesis.core.llm;

/**
 * Receives the incremental pieces of a streamed chat completion.
 *
 * <p>The provider invokes {@link #onToken(String)} for every text fragment as it
 * arrives, then {@link #onComplete()} once the stream is finished. Errors are
 * forwarded through {@link #onError(Throwable)} so callers can react.</p>
 */
public interface StreamHandler {
    void onToken(String token);

    void onComplete();

    default void onError(Throwable t) {
        throw new RuntimeException("Stream failed", t);
    }
}
