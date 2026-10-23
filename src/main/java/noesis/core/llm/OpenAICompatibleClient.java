package noesis.core.llm;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import noesis.core.json.Json;

/**
 * A streaming-capable client for any OpenAI-compatible chat endpoint
 * (OpenAI, Azure OpenAI, local servers such as Ollama or vLLM, and many
 * managed providers). It uses only the JDK {@link HttpClient}, so the framework
 * ships with zero external dependencies.
 */
public final class OpenAICompatibleClient implements LlmProvider {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient http;

    private OpenAICompatibleClient(Builder b) {
        this.baseUrl = b.baseUrl.endsWith("/") ? b.baseUrl : b.baseUrl + "/";
        this.apiKey = b.apiKey;
        this.model = b.model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ChatRequest req = withModel(request);
        String body = Json.write(req.toWire().build());
        HttpRequest httpReq = post(body, false);
        try {
            HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException(
                        "LLM endpoint returned " + resp.statusCode() + ": " + resp.body());
            }
            Map<String, Object> root = Json.parseObject(resp.body());
            return ChatResponse.fromJson(root);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chat request interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Chat request failed", e);
        }
    }

    @Override
    public void stream(ChatRequest request, StreamHandler handler) {
        ChatRequest req = withModel(request).toBuilder().stream(true).build();
        String body = Json.write(req.toWire().build());
        HttpRequest httpReq = post(body, true);
        try {
            HttpResponse<java.io.InputStream> resp =
                    http.send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() >= 400) {
                handler.onError(new IllegalStateException("LLM endpoint returned " + resp.statusCode()));
                return;
            }
            StringBuilder content = new StringBuilder();
            List<ToolCall> toolCalls = new ArrayList<>();
            SseParser.consume(new InputStreamReader(resp.body(), StandardCharsets.UTF_8), payload -> {
                Map<String, Object> chunk = Json.parseObject(payload);
                List<Object> choices = Json.getArray(chunk, "choices");
                if (choices == null) return;
                for (Object c : choices) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> choice = (Map<String, Object>) c;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> delta = Json.getObject(choice, "delta");
                    if (delta == null) continue;
                    String token = Json.getString(delta, "content");
                    if (token != null && !token.isEmpty()) {
                        content.append(token);
                        handler.onToken(token);
                    }
                }
            });
            handler.onComplete();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handler.onError(e);
        } catch (Exception e) {
            handler.onError(e);
        }
    }

    private ChatRequest withModel(ChatRequest request) {
        if (request.model() != null) return request;
        return request.toBuilder().model(model).build();
    }

    private HttpRequest post(String body, boolean stream) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "chat/completions"))
                .timeout(Duration.ofSeconds(stream ? 120 : 60))
                .header("Content-Type", "application/json")
                .header("Accept", stream ? "text/event-stream" : "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (apiKey != null && !apiKey.isEmpty()) {
            b.header("Authorization", "Bearer " + apiKey);
        }
        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey;
        private String model = "gpt-4o-mini";

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public OpenAICompatibleClient build() {
            return new OpenAICompatibleClient(this);
        }
    }
}
