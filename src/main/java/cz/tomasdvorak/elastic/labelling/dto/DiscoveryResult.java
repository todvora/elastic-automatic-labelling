package cz.tomasdvorak.elastic.labelling.dto;

import java.util.List;

public class DiscoveryResult {
    private final String text;
    private final long responseTime;
    private final List<TagHit> tags;

    public DiscoveryResult(final String text, final long responseTime, final List<TagHit> tags) {
        this.text = text;
        this.responseTime = responseTime;
        this.tags = tags;
    }

    public String getText() {
        return text;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public List<TagHit> getTags() {
        return tags;
    }
}
