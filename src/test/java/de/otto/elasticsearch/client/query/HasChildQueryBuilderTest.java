package de.otto.elasticsearch.client.query;

import com.google.gson.JsonObject;
import org.testng.annotations.Test;

import static de.otto.elasticsearch.client.request.GsonHelper.object;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class HasChildQueryBuilderTest {

    @Test
    public void shouldBuildHasChildQuery() throws Exception {
        // when
        JsonObject queryAsJson = new HasChildQueryBuilder("someType", QueryBuilders.matchAll()).build();

        //then
        JsonObject query = object("type", "someType");
        query.add("query", object("match_all", new JsonObject()));
        assertThat(queryAsJson, is(
                object("has_child",
                        query)));
    }


}