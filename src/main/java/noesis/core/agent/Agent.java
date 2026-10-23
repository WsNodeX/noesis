package noesis.core.agent;

import java.util.List;

import noesis.core.llm.ChatMessage;
import noesis.core.llm.ChatRequest;
import noesis.core.llm.ChatResponse;
import noesis.core.llm.LlmProvider;
import noesis.core.tools.ToolExecutor;
import noesis.core.tools.ToolRegistry;

/**
 * A reasoning agent that loops between a language model and a set of tools.
 *
 * <p>The loop is the classic ReAct pattern: the model is shown the conversation
 * and the available tool definitions; if it replies with tool calls, those are
 * executed and their results are appended; the cycle repeats until the model
 * produces a plain-text answer or the step budget is exhausted. Everything is
 * driven through the {@link LlmProvider} and {@link ToolRegistry} seams, so the
 * agent itself contains no network or framework-specific code.</p>
 */
public final class Agent {
    private final AgentConfig config;
    private final LlmProvider provider;
    private final ToolRegistry registry;
    private final ConversationMemory memory;

    public Agent(AgentConfig config, LlmProvider provider, ToolRegistry registry) {
        this(config, provider, registry, new ConversationMemory());
    }

    public Agent(AgentConfig config, LlmProvider provider, ToolRegistry registry, ConversationMemory memory) {
        this.config = config;
        this.provider = provider;
        this.registry = registry;
        this.memory = memory;
    }

    /** Seed the conversation with an optional system prompt. */
    public Agent system(String prompt) {
        memory.add(ChatMessage.system(prompt));
        return this;
    }

    /** Run the agent against a user query and return its final answer. */
    public String run(String query) {
        memory.add(ChatMessage.user(query));
        for (int step = 0; step < config.maxSteps(); step++) {
            ChatRequest request = ChatRequest.builder(config.model())
                    .messages(memory.all())
                    .tools(registry.functionDefs())
                    .build();
            ChatResponse response = provider.chat(request);
            ChatMessage assistant = response.firstMessage();
            if (assistant == null) break;

            if (assistant.hasToolCalls()) {
                memory.add(assistant);
                List<ChatMessage> results = ToolExecutor.execute(assistant.toolCalls(), registry);
                memory.addAll(results);
                continue;
            }
            String answer = assistant.content() == null ? "" : assistant.content();
            memory.add(ChatMessage.assistant(answer));
            return answer;
        }
        return config.exhaustedMessage();
    }

    /** The full transcript so far (handy for debugging or logging). */
    public List<ChatMessage> transcript() {
        return memory.all();
    }
}
