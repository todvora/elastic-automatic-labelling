package cz.tomasdvorak.elastic.labelling.elastic;

import cz.tomasdvorak.elastic.labelling.dto.DiscoveryResult;
import cz.tomasdvorak.elastic.labelling.dto.TagHit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class ElasticPrecolateIndex {

    private static final String INDEX_NAME = "precolate-tags";

    private static final Logger logger = LogManager.getLogger(ElasticPrecolateIndex.class);

    @Autowired
    private ElasticClient elastic;

    public DiscoveryResult discoverTags(final String inputText) throws IOException {
        //Build a document to check against the percolator
        XContentBuilder docBuilder = XContentFactory.jsonBuilder().startObject();
        docBuilder.field("content", inputText);
        docBuilder.endObject(); //End of the JSON root object

        PercolateQueryBuilder percolateQuery = new PercolateQueryBuilder("query", "docs", docBuilder.bytes());

        // Percolate, by executing the percolator query in the query dsl:
        SearchResponse response = elastic.getClient().prepareSearch(INDEX_NAME)
                .setQuery(percolateQuery)
                .highlighter(new HighlightBuilder().field("content").numOfFragments(1))
                .get();
        //Iterate over the results
        final List<TagHit> tagHits = StreamSupport.stream(response.getHits().spliterator(), false)
                .map(h -> new TagHit((String) h.getSource().get("value"), h.getScore(), getHighlightedText(h)))
                .collect(Collectors.toList());
        return new DiscoveryResult(inputText, response.getTookInMillis(), tagHits);
    }

    private String getHighlightedText(final SearchHit h) {
        final HighlightField field = h.getHighlightFields().get("content");
        if (field.fragments().length > 0) {
            return field.fragments()[0].string();
        }
        return "";
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
                    .addMapping("docs", "content", "type=text,analyzer=english")
                    .get();
        }


    }

    private RestStatus indexQuery(final String tag) {

        logger.info("Indexing precolate query for tag " + tag);

        //This is the query we're registering in the percolator
        QueryBuilder qb = QueryBuilders.matchPhraseQuery("content", tag).analyzer("english");


        //Index the query = register it in the percolator
        final IndexResponse query = elastic.getClient().prepareIndex(INDEX_NAME, "query")
                .setSource(buildQueryObject(qb, tag))
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE) // Needed when the query shall be available immediately
                .get();
        final RestStatus status = query.status();
        return status;
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
