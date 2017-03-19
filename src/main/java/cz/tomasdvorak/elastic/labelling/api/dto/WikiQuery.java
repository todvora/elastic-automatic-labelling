package cz.tomasdvorak.elastic.labelling.api.dto;

import java.util.Map;

public class WikiQuery {
    private Map<Long, WikiPage> pages;

    public Map<Long, WikiPage> getPages() {
        return pages;
    }

    public void setPages(final Map<Long, WikiPage> pages) {
        this.pages = pages;
    }
}
