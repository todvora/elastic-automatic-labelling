package cz.tomasdvorak.elastic.labelling.dto;

public class TagHit {
    private final String value;
    private final float score;
    private final String highlighted;

    public TagHit(final String value, final float score, final String highlighted) {
        this.value = value;
        this.score = score;
        this.highlighted = highlighted;
    }

    public String getValue() {
        return value;
    }

    public float getScore() {
        return score;
    }

    public String getHighlighted() {
        return highlighted;
    }
}
