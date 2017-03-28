package cz.tomasdvorak.elastic.labelling.elastic;

import cz.tomasdvorak.elastic.labelling.dto.ElasticHits;
import cz.tomasdvorak.elastic.labelling.dto.TagHit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ElasticPercolateIndex {

    private static final String INDEX_NAME = "percolate-tags";
    private static final String ANALYZER = "english";

    private static final Logger logger = LogManager.getLogger(ElasticPercolateIndex.class);

    private final ElasticClient elastic;

    @Autowired
    public ElasticPercolateIndex(final ElasticClient elastic) {
        this.elastic = elastic;
    }

    public ElasticHits discoverTags(final String inputText) throws IOException {

        XContentBuilder docBuilder = XContentFactory.jsonBuilder().startObject();
        docBuilder.field("content", inputText);
        docBuilder.endObject();

        PercolateQueryBuilder percolateQuery = new PercolateQueryBuilder("query", "docs", docBuilder.bytes());

        // Percolate, by executing the percolator query in the query dsl:
        SearchResponse response = elastic.getClient().prepareSearch(INDEX_NAME)
                .setQuery(percolateQuery)
                .highlighter(new HighlightBuilder().field("content"))
                .setSize(10) // get only first ten results
                .get();

        //Iterate over the results
        final List<TagHit> tagHits = StreamSupport.stream(response.getHits().spliterator(), false)
                .map(h -> new TagHit((String) h.getSource().get("value"), h.getScore(), getHighlightedText(h)))
                .collect(Collectors.toList());
        return new ElasticHits(inputText, response.getTookInMillis(), response.getHits().getTotalHits(), response.getHits().getMaxScore(), tagHits);
    }

    private String getHighlightedText(final SearchHit h) {
        return h.getHighlightFields().values().stream()
                .flatMap(f -> Arrays.stream(f.fragments()))
                .map(Text::string)
                .collect(Collectors.joining("…"));
    }

    public void addTags(final Collection<String> keywords) throws IOException {
        keywords.stream().parallel().forEach(this::indexQuery);
    }

    public Collection<String> getTags() throws IOException {
        Set<String> result = new HashSet<>();
        SearchResponse scrollResp = elastic.getClient().prepareSearch(INDEX_NAME)
                .addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
                .setQuery(QueryBuilders.matchAllQuery())
                .setScroll(new TimeValue(60000))
                .setSize(100).execute().actionGet();
        do {
            for (SearchHit hit : scrollResp.getHits().getHits()) {
                result.add((String) hit.getSource().get("value"));
            }
            scrollResp = elastic.getClient().prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
        } while (scrollResp.getHits().getHits().length != 0);
        return result;
    }

    @PostConstruct
    private void ensureIndex() {
        final IndicesExistsResponse indicesExistsResponse = elastic.getClient().admin().indices().prepareExists(INDEX_NAME).get();
        if (!indicesExistsResponse.isExists()) {
            // create an index with a percolator field with the name 'query':
            elastic.getClient().admin().indices().prepareCreate(INDEX_NAME)
                    .addMapping("query", "query", "type=percolator")
                    .addMapping("docs", "content", "type=text,analyzer=" + ANALYZER)
                    .get();
        }


    }

    private RestStatus indexQuery(final String tag) {

        logger.info("Indexing precolate query for tag " + tag);

        //This is the query we're registering in the percolator
        QueryBuilder qb = QueryBuilders.matchPhraseQuery("content", tag);


        //Index the query = register it in the percolator
        final IndexResponse query = elastic.getClient().prepareIndex(INDEX_NAME, "query")
                .setSource(buildQueryObject(qb, tag))
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE) // Needed when the query shall be available immediately
                .get();
        return query.status();
    }

    private XContentBuilder buildQueryObject(final QueryBuilder qb, final String tag) {
        try {
            return XContentFactory.jsonBuilder()
                    .startObject()
                    .field("query", qb) // Register the query
                    .field("value", tag) // Register the value of the tag
                    .endObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void dropCreate() {
        elastic.getClient().admin().indices().prepareDelete(INDEX_NAME).get();
        ensureIndex();
    }
}
