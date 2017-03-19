package cz.tomasdvorak.elastic.labelling.api.dto;

public class WikiPage {
    private String title;
    private String extract;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getExtract() {
        return extract;
    }

    public void setExtract(final String extract) {
        this.extract = extract;
    }

    public String getUrl() {
        return "https://en.wikipedia.org/wiki/" + title;
    }
}
