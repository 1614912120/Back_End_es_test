package cn.itcast.hotel;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.service.IHotelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class HotelDocumentTest {
    @Autowired
    private IHotelService hotelService;
    private RestHighLevelClient client;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.101.65:9200")
        ));
    }


    @AfterEach
    void testDown() throws IOException {
        this.client.close();
    }

    //新增文档
    @Test
    public void testAddDocument() throws Exception {
        Hotel hotel = hotelService.getById(61083L);
        HotelDoc hotelDoc = new HotelDoc(hotel);
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(hotelDoc);
        // 1.准备Request对象
        IndexRequest request = new IndexRequest("hotel").id(hotelDoc.getId().toString());
        // 2.准备Json文档
        request.source(json, XContentType.JSON);
        client.index(request,RequestOptions.DEFAULT);
    }

    //查询文档
    @Test
    void testGetDocumentById() throws IOException {
        GetRequest request = new GetRequest("hotel", "61083");
        //发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        String json = response.getSourceAsString();
        ObjectMapper mapper = new ObjectMapper();
        HotelDoc hotelDoc = mapper.readValue(json, HotelDoc.class);
        System.out.println(hotelDoc);
    }

    //删除文档
    @Test
    void testDeleteDocument() throws IOException {
        DeleteRequest request = new DeleteRequest("hotel", "61083");
        client.delete(request,RequestOptions.DEFAULT);
    }

    //增量修改
    @Test
    void testUpdateDocument() throws IOException {
        UpdateRequest request = new UpdateRequest("hotel", "61083");
        request.doc(
                "price", "952",
                "starName", "四钻"
        );

        client.update(request,RequestOptions.DEFAULT);
    }

    //批量导入文档
    @Test
    void testBulk() throws Exception {
        List<Hotel> hotels = hotelService.list();
        ObjectMapper mapper = new ObjectMapper();
        //创建request
        BulkRequest request = new BulkRequest();
        for (Hotel hotel : hotels) {
            HotelDoc hotelDoc = new HotelDoc(hotel);
            //创建新增文档的request对象
            request.add(new IndexRequest("hotel")
                    .id(hotelDoc.getId().toString())
                    .source(mapper.writeValueAsString(hotelDoc),XContentType.JSON));
        }
        client.bulk(request,RequestOptions.DEFAULT);
    }


    //查询请求
    @Test
    void testMatchAll() throws Exception {
        SearchRequest request = new SearchRequest("hotel");
        request.source()
                .query(QueryBuilders.matchAllQuery());
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    private void handleResponse(SearchResponse response) throws Exception {
        SearchHits searchHits = response.getHits();
        long total = searchHits.getTotalHits().value;
        System.out.println("数据="+total);

        //文档数组
        SearchHit[] hits = searchHits.getHits();
        ObjectMapper mapper = new ObjectMapper();
        for (SearchHit hit : hits) {
            //获取文档source
            String json = hit.getSourceAsString();
            HotelDoc hotelDoc = mapper.readValue(json, HotelDoc.class);
            System.out.println("hotel="+hotelDoc);
        }
    }

    //match查询
    @Test
    void testMatch () throws Exception {
        SearchRequest request = new SearchRequest("hotel");
        request.source()
                .query(QueryBuilders.matchQuery("all","如家"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }

    //精确查询 | 布尔查询
    @Test
    void testBool() throws Exception {
        // 1.准备Request
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备DSL
        // 2.1.准备BooleanQuery
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 2.2.添加term
        boolQuery.must(QueryBuilders.termQuery("city", "杭州"));
        // 2.3.添加range
        boolQuery.filter(QueryBuilders.rangeQuery("price").lte(250));

        request.source().query(boolQuery);
        // 3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析响应
        handleResponse(response);
    }


    //排序分页
    @Test
    void testPageAndSort() throws Exception {
        int page = 1,size = 5;
        SearchRequest request = new SearchRequest("hotel");
        //准备dsl
        request.source().query(QueryBuilders.matchAllQuery());
        request.source().sort("price", SortOrder.ASC);
        //分页
        request.source().from((page-1)*size).size(5);
        //发请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponse(response);
    }


    //高亮
    @Test
    void testHighLight() throws Exception {
        SearchRequest request = new SearchRequest("hotel");
        //dsl
        request.source().query(QueryBuilders.matchQuery("all","如家"));
        request.source()
                .highlighter(new HighlightBuilder().field("name")
                        .requireFieldMatch(false));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        handleResponses(response);
    }

    private void handleResponses(SearchResponse response) throws Exception {
        SearchHits searchHits = response.getHits();
        ObjectMapper mapper = new ObjectMapper();
        long total = searchHits.getTotalHits().value;
        System.out.println("获取总数="+total);
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String json = hit.getSourceAsString();
            //反序列化
            HotelDoc hotelDoc = mapper.readValue(json, HotelDoc.class);
            //获取高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if(!CollectionUtils.isEmpty(highlightFields)) {
                HighlightField highlightField = highlightFields.get("name");
                if(highlightField != null) {
                    String name = highlightField.getFragments()[0].toString();
                    hotelDoc.setName(name);
                }
            }
            System.out.println("hoteldoc="+hotelDoc);
        }
    }
}
