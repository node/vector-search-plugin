package org.elasticsearch;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.cluster.routing.UnassignedInfo;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.test.ESIntegTestCase;
import org.junit.Test;


public class EsPluginTests extends ESIntegTestCase {

    private static final String INDEX_NAME = "test-index";
    private static final String TYPE_NAME = "test-type";

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singletonList(VectorSearchPlugin.class);
    }

    private XContentBuilder exampleSettings() throws IOException {
        return jsonBuilder().startObject()
                .field(SETTING_NUMBER_OF_SHARDS, numberOfShards())
                .field(SETTING_NUMBER_OF_REPLICAS, numberOfReplicas())
                .field(UnassignedInfo.INDEX_DELAYED_NODE_LEFT_TIMEOUT_SETTING.getKey(), 0)
                .endObject();
    }

    private XContentBuilder exampleMapping() throws IOException {
        return jsonBuilder()
            .startObject()
                .startObject("properties")
                    .startObject("v")
                        .field("type", "vector")
                        .field("dimensions", "4")
                    .endObject()
                .endObject()
            .endObject();
    }

    private void doCreateIndex() throws IOException {
        client().admin().indices().prepareCreate(INDEX_NAME).setSettings(exampleSettings()).addMapping(TYPE_NAME, exampleMapping()).get();
    }

    private XContentBuilder exampleDocument0() throws IOException {
        return jsonBuilder().startObject().field("v", "0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0").endObject();
    }

    private XContentBuilder exampleDocument0_5() throws IOException {
        return jsonBuilder().startObject().field("v", "0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5").endObject();
    }

    private XContentBuilder exampleDocument2() throws IOException {
        return jsonBuilder().startObject().field("v", "2.0,2.0,2.0,2.0,2.0,2.0,2.0,2.0").endObject();
    }

    @Test
    public void smokeTest() throws IOException {
        doCreateIndex();
        ensureGreen(INDEX_NAME);
        indexExists(INDEX_NAME);
    }

    @Test
    public void theFieldWorks() throws IOException {
        doCreateIndex();

        client().prepareIndex(INDEX_NAME, TYPE_NAME).setSource(exampleDocument0()).get();
        client().prepareIndex(INDEX_NAME, TYPE_NAME).setSource(exampleDocument0_5()).get();
        client().prepareIndex(INDEX_NAME, TYPE_NAME).setSource(exampleDocument2()).get();

        refresh(INDEX_NAME);

        SearchResponse response = client().prepareSearch(INDEX_NAME).setExplain(true).setQuery(rangeQuery("v").from("-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0").to("1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0")).get();

        assertEquals(2, response.getHits().totalHits);

        SearchHit hit = response.getHits().getHits()[0];
    }

}
