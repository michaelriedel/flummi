package de.otto.elasticsearch.client.query;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import static de.otto.elasticsearch.client.request.GsonHelper.object;

public class HasChildQueryBuilder implements QueryBuilder {
    private final String type;
    private final QueryBuilder query;

    public HasChildQueryBuilder(String type, QueryBuilder query) {
        this.type = type;
        this.query = query;
    }

    @Override
    public JsonObject build() {
        JsonObject hasChild = new JsonObject();
        hasChild.add("type", new JsonPrimitive(type));
        hasChild.add("query", query.build());
        return object(
                "has_child",
                hasChild);
    }
}
