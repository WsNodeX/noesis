package noesis.core.agent;

/** Tunable parameters for an {@link Agent}. */
public final class AgentConfig {
    private final String model;
    private final int maxSteps;
    private final String exhaustedMessage;

    private AgentConfig(Builder b) {
        this.model = b.model;
        this.maxSteps = b.maxSteps;
        this.exhaustedMessage = b.exhaustedMessage;
    }

    public String model() {
        return model;
    }

    public int maxSteps() {
        return maxSteps;
    }

    public String exhaustedMessage() {
        return exhaustedMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String model = "gpt-4o-mini";
        private int maxSteps = 8;
        private String exhaustedMessage = "I could not reach a final answer within the step budget.";

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder exhaustedMessage(String exhaustedMessage) {
            this.exhaustedMessage = exhaustedMessage;
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(this);
        }
    }
}
