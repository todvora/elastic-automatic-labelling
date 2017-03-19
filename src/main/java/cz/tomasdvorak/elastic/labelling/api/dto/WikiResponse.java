package cz.tomasdvorak.elastic.labelling.api.dto;

public class WikiResponse {
    private WikiQuery query;

    public WikiQuery getQuery() {
        return query;
    }

    public void setQuery(final WikiQuery query) {
        this.query = query;
    }
}
