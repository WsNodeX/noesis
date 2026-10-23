package noesis.examples;

import java.util.List;
import java.util.Map;

import noesis.Noesis;
import noesis.core.agent.Agent;
import noesis.core.agent.AgentConfig;
import noesis.core.json.Json;
import noesis.core.llm.ChatMessage;
import noesis.core.llm.ChatResponse;
import noesis.core.llm.Role;
import noesis.core.llm.ToolCall;
import noesis.core.rag.Answer;
import noesis.core.rag.RagPipeline;
import noesis.core.retrieval.HashEmbedding;
import noesis.core.tools.Tool;
import noesis.core.tools.ToolRegistry;
import noesis.core.tools.ToolSpec;

/**
 * Self-contained, offline demonstration of the framework.
 *
 * <p>Run it with {@code java noesis.examples.Demo} (after compiling). It shows
 * a tool-calling agent solving a maths problem and a RAG pipeline answering a
 * question from a small private corpus &mdash; both using the scripted
 * {@link StubProvider} so no API key or network access is required.</p>
 */
public final class Demo {

    public static void main(String[] args) {
        System.out.println(Noesis.banner());
        System.out.println("=".repeat(64));
        runAgentDemo();
        System.out.println("=".repeat(64));
        runRagDemo();
    }

    private static void runAgentDemo() {
        System.out.println("\n[1] Tool-calling agent");
        ToolRegistry registry = new ToolRegistry().register(calculator());

        // Script the "model": first ask for a calculation, then answer.
        StubProvider provider = new StubProvider();
        provider.then(toolCall("call_1", "calculator", Map.of("a", 7, "b", 5, "op", "mul")))
                .fallback(ChatResponse.builder()
                        .choices(List.of(ChatMessage.assistant("7 multiplied by 5 equals 35.")))
                        .build());

        Agent agent = new Agent(AgentConfig.builder().model("stub").maxSteps(6).build(), provider, registry)
                .system("You are a careful maths assistant. Use the calculator when needed.");

        String answer = agent.run("What is 7 times 5?");
        System.out.println("  User   : What is 7 times 5?");
        System.out.println("  Tool   : calculator(7, 5, mul) -> 35");
        System.out.println("  Agent  : " + answer);
    }

    private static void runRagDemo() {
        System.out.println("\n[2] Retrieval-augmented generation");
        StubProvider provider = new StubProvider().fallback(
                ChatResponse.builder().choices(List.of(ChatMessage.assistant(
                        "Aurora supports streaming responses and tool "
                                + "calling, as stated in passages [1] and [2]."))).build());

        RagPipeline rag = new RagPipeline(new HashEmbedding(), provider, "stub");
        rag.ingest("spec", "Aurora is a lightweight inference server written in Rust. "
                + "It exposes an OpenAI-compatible chat endpoint.");
        rag.ingest("spec", "Aurora supports streaming responses over Server-Sent Events "
                + "and can call external tools through function calling.");
        rag.ingest("spec", "Aurora scales horizontally and ships with a built-in tokenizer "
                + "cache to reduce cold-start latency.");

        Answer answer = rag.answer("Does Aurora support streaming and tool calling?");
        System.out.println("  Indexed chunks : " + rag.indexedChunks());
        System.out.println("  Top sources    : " + rag.describeSources(answer.sources()));
        System.out.println("  Answer         : " + answer.content());
    }

    private static ToolSpec calculator() {
        Map<String, Object> schema = Json.object()
                .put("type", "object")
                .put("properties", Json.object()
                        .put("a", Json.object().put("type", "number").build())
                        .put("b", Json.object().put("type", "number").build())
                        .put("op", Json.object()
                                .put("type", "string")
                                .put("enum", Json.array().add("add").add("sub").add("mul").add("div").build())
                                .build())
                        .build())
                .put("required", Json.array().add("a").add("b").add("op").build())
                .build();
        Tool handler = args -> {
            double a = ((Number) args.get("a")).doubleValue();
            double b = ((Number) args.get("b")).doubleValue();
            String op = String.valueOf(args.get("op"));
            return switch (op) {
                case "add" -> String.valueOf(a + b);
                case "sub" -> String.valueOf(a - b);
                case "mul" -> String.valueOf(a * b);
                case "div" -> String.valueOf(a / b);
                default -> throw new IllegalArgumentException("unknown op " + op);
            };
        };
        return ToolSpec.of("calculator", "Multiply, add, subtract or divide two numbers.", schema, handler);
    }

    private static ChatResponse toolCall(String id, String name, Map<String, Object> args) {
        String json = Json.write(args);
        ChatMessage message = ChatMessage.builder(Role.ASSISTANT)
                .toolCalls(List.of(new ToolCall(id, name, json)))
                .build();
        return ChatResponse.builder().choices(List.of(message)).build();
    }
}
