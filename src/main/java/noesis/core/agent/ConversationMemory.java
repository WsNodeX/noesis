package noesis.core.agent;

import java.util.ArrayList;
import java.util.List;

import noesis.core.llm.ChatMessage;

/**
 * The running transcript of an agent's conversation.
 *
 * <p>It holds the system prompt, the user turns, the assistant turns (including
 * any tool-call requests) and the tool results. When the number of retained
 * turns exceeds {@code maxTurns}, the oldest non-system turns are dropped so
 * very long sessions stay within the model's context window.</p>
 */
public final class ConversationMemory {
    private final List<ChatMessage> messages = new ArrayList<>();
    private final int maxTurns;

    public ConversationMemory() {
        this(40);
    }

    public ConversationMemory(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public void add(ChatMessage message) {
        messages.add(message);
        evict();
    }

    public void addAll(List<ChatMessage> more) {
        messages.addAll(more);
        evict();
    }

    public List<ChatMessage> all() {
        return List.copyOf(messages);
    }

    public int size() {
        return messages.size();
    }

    private void evict() {
        if (maxTurns <= 0) return;
        // Keep the first message if it is the system prompt; evict from the top.
        int start = (!messages.isEmpty() && messages.get(0).role() == noesis.core.llm.Role.SYSTEM) ? 1 : 0;
        while (messages.size() - start > maxTurns) {
            messages.remove(start);
        }
    }
}
