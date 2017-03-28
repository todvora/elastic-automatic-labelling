package cz.tomasdvorak.elastic.labelling.api;

import cz.tomasdvorak.elastic.labelling.api.dto.WikiPage;
import cz.tomasdvorak.elastic.labelling.api.dto.WikiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Wikipedia {

    private static final String API_URL = "https://en.wikipedia.org/w/api.php?action=query&generator=random&grnnamespace=0&prop=extracts&format=json&explaintext";

    private final RestTemplate restTemplate;

    @Autowired
    public Wikipedia(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WikiPage getRandomPage() {
        WikiResponse response = restTemplate.getForObject(API_URL, WikiResponse.class);
        return response.getQuery().getPages().entrySet().iterator().next().getValue();
    }
}
