package de.otto.flummi;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.otto.flummi.aggregations.NestedAggregationBuilder;
import de.otto.flummi.aggregations.TermsBuilder;
import de.otto.flummi.query.QueryBuilders;
import de.otto.flummi.request.SearchRequestBuilder;
import de.otto.flummi.response.AggregationResult;
import de.otto.flummi.response.ScrollingSearchHits;
import de.otto.flummi.response.SearchHit;
import de.otto.flummi.response.SearchResponse;
import de.otto.flummi.util.HttpClientWrapper;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.util.concurrent.TimeoutException;

import static de.otto.flummi.SortOrder.ASC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class SearchRequestBuilderTest {

    public static final String EMPTY_SEARCH_RESPONSE = "{\"took\":1, \"hits\":{\"max_score\":null,\"total\":0, \"hits\":[]}}";
    public static final String PRODUCT_JSON = "{\"_id\":\"P0\",\"lastModified\":\"2016-04-14T09:17:46.888\",\"topProductScore\":0.0,\"promotable\":true,\"soldout\":false,\"sale\":false,\"largeSize\":false,\"premiumBrand\":false,\"sharedCategoryPath\":\"some\",\"brand\":\"adidas\",\"categories\":[{\"path\":\"some/path/p0/\",\"titlePath\":\"Some\\u003ePath\\u003eP0\",\"assortment\":\"spielzeug\",\"targetGroup\":\"herren\",\"uniqueId\":\"Some\\u003ePath\\u003eP0§spielzeug§herren\"},{\"path\":\"some/other/path/p0/\",\"titlePath\":\"Some\\u003eOther\\u003ePath\\u003eP0\",\"assortment\":\"sport\",\"targetGroup\":\"herren\",\"uniqueId\":\"Some\\u003eOther\\u003ePath\\u003eP0§sport§herren\"}],\"levelCategories\":{\"1\":{\"value\":[\"p0\",\"path\"],\"count\":2},\"2\":{\"value\":[\"p0\"],\"count\":1}},\"tags\":[],\"reductionClasses\":[],\"displayRestrictions\":[]}";
    public static final String SEARCH_RESPONSE_WITH_ONE_HIT = "{\"took\":1," +
            "\"timed_out\":false," +
            "\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0}," +
            "\"hits\":{\"total\":1,\"max_score\":1.0,\"hits\":[" +
            "{\"_index\":\"product_1460618266743\",\"_type\":\"product\",\"_id\":\"P0\",\"_score\":1.0,\"_source\":" +
            PRODUCT_JSON +
            "}" +
            "]}}";
    public static final String SEARCH_RESPONSE_WITH_ONE_HIT_VERSION_7 = "{\"took\":1," +
            "\"timed_out\":false," +
            "\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0}," +
            "\"hits\":{\"total\":{\"value\":1,\"relation\":\"eq\"},\"max_score\":1.0,\"hits\":[" +
            "{\"_index\":\"product_1460618266743\",\"_type\":\"product\",\"_id\":\"P0\",\"_score\":1.0,\"_source\":" +
            PRODUCT_JSON +
            "}" +
            "]}}";
    public static final String SEARCH_RESPONSE_WITH_AGGREGATION = "{\"took\":85," +
            "\"timed_out\":false," +
            "\"_shards\":{\"total\":1,\"successful\":1,\"failed\":0}," +
            "\"hits\":{\"total\":2,\"max_score\":0.0,\"hits\":[]}," +
            "\"aggregations\":{" +
            "\"categories_distinct\":{" +
            "\"doc_count\":2," +
            "\"categories_unique_id_distinct\":{" +
            "\"doc_count_error_upper_bound\":0," +
            "\"sum_other_doc_count\":0," +
            "\"buckets\":[{\"key\":\"Spielzeug>Path>More§spielzeug§damen\",\"doc_count\":1},{\"key\":\"Spielzeug>Path§spielzeug§herren\",\"doc_count\":1}]}}}}";

    public static final String SEARCH_RESPONSE_WITH_SCROLL_ID = "{\"took\":1," +
            "\"_scroll_id\":\"some_scroll_id\"," +
            "\"timed_out\":false," +
            "\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0}," +
            "\"hits\":{\"total\":10,\"max_score\":1.0,\"hits\":[" +
            "{\"_index\":\"product_1460618266743\",\"_type\":\"product\",\"_id\":\"P0\",\"_score\":1.0,\"_source\":" +
            PRODUCT_JSON +
            "}," +
            "{\"_index\":\"product_1460618266743\",\"_type\":\"product\",\"_id\":\"P1\",\"_score\":1.0,\"_source\":" +
            PRODUCT_JSON +
            "}" +
            "]}}";
    SearchRequestBuilder searchRequestBuilder;
    HttpClientWrapper httpClient;

    @BeforeMethod
    public void setUp() throws Exception {
        httpClient = mock(HttpClientWrapper.class);
        searchRequestBuilder = new SearchRequestBuilder(httpClient, "some-index");
    }

    @Test
    public void shouldBuilderQueryWithStoredField() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).addStoredField("someField").execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}},\"stored_fields\":[\"someField\"]}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldBuilderQueryWithSourceFilter() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).addSourceFilter("someField").execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}},\"_source\":[\"someField\"]}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldParseSearchResponseWithFullDocuments() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", SEARCH_RESPONSE_WITH_ONE_HIT)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}}}");
        verify(httpClient).preparePost("/some-index/_search");

        assertThat(response.getTookInMillis(), is(1L));
        assertThat(response.getHits().getMaxScore(), is(1F));
        assertThat(response.getAggregations().size(), is(0));
        assertThat(response.getHits().getTotalHits(), is(1L));
        SearchHit firstHit = response.getHits().iterator().next();
        assertThat(firstHit.getFields().entrySet().size(), is(0));
        assertThat(firstHit.getId(), is("P0"));
        assertThat(firstHit.getScore(), is(1F));
        assertThat(firstHit.getSource(), is(new Gson().fromJson(PRODUCT_JSON, JsonObject.class)));
    }

    @Test
    public void shouldParseSearchResponseWithFullDocumentsForEsVersion7() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", SEARCH_RESPONSE_WITH_ONE_HIT_VERSION_7)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}}}");
        verify(httpClient).preparePost("/some-index/_search");

        assertThat(response.getTookInMillis(), is(1L));
        assertThat(response.getHits().getMaxScore(), is(1F));
        assertThat(response.getAggregations().size(), is(0));
        assertThat(response.getHits().getTotalHits(), is(1L));
        SearchHit firstHit = response.getHits().iterator().next();
        assertThat(firstHit.getFields().entrySet().size(), is(0));
        assertThat(firstHit.getId(), is("P0"));
        assertThat(firstHit.getScore(), is(1F));
        assertThat(firstHit.getSource(), is(new Gson().fromJson(PRODUCT_JSON, JsonObject.class)));
    }

    @Test
    public void shouldParseSearchResponseWithAggregations() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", SEARCH_RESPONSE_WITH_AGGREGATION)));

        // when
        SearchResponse response = searchRequestBuilder
                .addAggregation(
                        new NestedAggregationBuilder("categories_distinct")
                                .path("categories")
                                .subAggregation(
                                        new TermsBuilder("categories_unique_id_distinct")
                                                .field("categories.uniqueId")
                                                .size(10)
                                ))
                .setQuery(createSampleQuery()).execute();

        //then
        verify(httpClient).preparePost("/some-index/_search");

        assertThat(response.getAggregations().size(), is(1));
        AggregationResult categoriesAggregation = response.getAggregations().get("categories_distinct");
        assertThat(categoriesAggregation.getNestedAggregations().get("categories_unique_id_distinct").getBuckets(), hasSize(2));
    }

    @Test
    public void shouldBuilderQueryWithFromAndSize() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).setSize(42).setFrom(111).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}},\"from\":111,\"size\":42}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldSetHttpRequestTimeout() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).setTimeoutMillis(54321).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}}}");
        verify(boundRequestBuilderMock).setRequestTimeout(54321);
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldBuildRequestWithOneSort() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).addSort("someKey", SortOrder.DESC).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}},\"sort\":[{\"someKey\":{\"order\":\"desc\"}}]}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldBuildRequestWithMultipleSort() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).addSort("someKey", SortOrder.DESC).addSort("someOtherKey", ASC).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}},\"sort\":[{\"someKey\":{\"order\":\"desc\"}},{\"someOtherKey\":{\"order\":\"asc\"}}]}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldBuildRequestWithPostFilter() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).setPostFilter(QueryBuilders.termQuery("someField", "someValue")).execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}},\"post_filter\":{\"term\":{\"someField\":\"someValue\"}}}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldBuildRequestWithAggregations() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery()).addAggregation(new TermsBuilder("Katzenklo")
                .field("someField"))
                .execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}}," +
                "\"aggregations\":{\"Katzenklo\":{\"terms\":{\"field\":\"someField\"}}}}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldBuildRequestWithScroll() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", SEARCH_RESPONSE_WITH_SCROLL_ID)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery())
                .setScroll("1m")
                .execute();

        //then
        verify(boundRequestBuilderMock).setBody("{\"query\":{\"term\":{\"someField\":\"someValue\"}}}");
        verify(httpClient).preparePost("/some-index/_search");
        verify(boundRequestBuilderMock).addQueryParam("scroll", "1m");
        assertThat(response.getScrollId(), is("some_scroll_id"));
    }

    @Test
    public void shouldBuildRequestWithNestedAggregations() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", EMPTY_SEARCH_RESPONSE)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery())
                .addAggregation(new NestedAggregationBuilder("Katzenklo")
                        .path("somePath")
                        .subAggregation(new TermsBuilder("someAggregation").field("someField"))
                )
                .execute();

        //then
        verify(boundRequestBuilderMock).setBody(
                "{" +
                        "\"query\":{\"term\":{\"someField\":\"someValue\"}}," +
                        "\"aggregations\":{" +
                        "\"Katzenklo\":{" +
                        "\"nested\":{\"path\":\"somePath\"}," +
                        "\"aggregations\":{" +
                        "\"someAggregation\":{" +
                        "\"terms\":{\"field\":\"someField\"}" +
                        "}" +
                        "}" +
                        "}" +
                        "}" +
                        "}");
        verify(httpClient).preparePost("/some-index/_search");
    }

    @Test
    public void shouldScroll() throws Exception {
        // given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.execute()).thenReturn(new CompletedFuture<>(new MockResponse(200, "ok", SEARCH_RESPONSE_WITH_SCROLL_ID)));

        // when
        SearchResponse response = searchRequestBuilder.setQuery(createSampleQuery())
                .setScroll("1m")
                .execute();

        //then
        assertThat(response.getHits().getClass().getName(), is(ScrollingSearchHits.class.getName()));
    }

    @Test
    public void shouldNotThrowNPExceptionIfScrollIdIsNotInSearchResponse() throws Exception {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson("{\"took\":3,\"timed_out\":false,\"_shards\":{\"total\":5,\"successful\":5,\"failed\":0},\"hits\":{\"total\":2,\"max_score\":1.0,\"hits\":[{\"_index\":\"salesorders\",\"_type\":\"salesorders\",\"_id\":\"AVoUU6q__Si-JRVFBPXC\",\"_score\":1.0,\"_source\":{\"quoteNumber\":\"11111111111\",\"quoteReference\":\"blablabla\",\"postCode\":\"RH41EA\"}},{\"_index\":\"salesorders\",\"_type\":\"salesorders\",\"_id\":\"AVoUU764_Si-JRVFBPXD\",\"_score\":1.0,\"_source\":{\"quoteNumber\":\"11111111111\",\"quoteReference\":\"blablabla\",\"postCode\":\"RH41EA\"}}]}}", JsonObject.class);
        SearchResponse.Builder response = searchRequestBuilder.parseResponse(jsonResponse, "1m", null);
        assertThat(response, is(notNullValue()));

    }

    @Test
    public void shouldAddQueryToException() throws Exception {
        //given
        BoundRequestBuilder boundRequestBuilderMock = mock(BoundRequestBuilder.class);
        when(httpClient.preparePost("/some-index/_search")).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setBody(any(String.class))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.setCharset(Charset.forName("UTF-8"))).thenReturn(boundRequestBuilderMock);
        when(boundRequestBuilderMock.addHeader(anyString(),anyString())).thenReturn(boundRequestBuilderMock);
        TimeoutException timeoutException = new TimeoutException();
        when(boundRequestBuilderMock.execute()).thenReturn(new ListenableFuture.CompletedFailure<>(timeoutException));

        //when
        JsonObject query = createSampleQuery();
        try {
            searchRequestBuilder.setQuery(query).execute();
        } catch (RuntimeException e) {
            //then
            assertThat(e.getCause().getCause(), is(timeoutException));
            assertThat(e.getMessage(), is("{\"query\":" + query.toString() + "}"));
        }

    }

    private JsonObject createSampleQuery() {
        return QueryBuilders.termQuery("someField", "someValue").build();
    }
}
