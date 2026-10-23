package noesis.core.retrieval;

/** A retrieved document together with its relevance score. */
public final class ScoredDocument {
    private final Document document;
    private final double score;

    public ScoredDocument(Document document, double score) {
        this.document = document;
        this.score = score;
    }

    public Document document() {
        return document;
    }

    public double score() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("ScoredDocument{score=%.4f, %s}", score, document);
    }
}
