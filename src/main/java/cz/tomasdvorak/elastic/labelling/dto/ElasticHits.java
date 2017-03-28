package cz.tomasdvorak.elastic.labelling.dto;

import java.util.List;

public class ElasticHits {
    private final String text;
    private final long responseTime;
    private final float maxScore;
    private final List<TagHit> tags;
    private final long totalHits;

    public ElasticHits(final String text, final long responseTime, final long totalHits, final float maxScore, final List<TagHit> tags) {
        this.text = text;
        this.responseTime = responseTime;
        this.totalHits = totalHits;
        this.maxScore = maxScore;
        this.tags = tags;
    }

    public String getText() {
        return text;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public float getMaxScore() {
        return maxScore;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public List<TagHit> getTags() {
        return tags;
    }
}
