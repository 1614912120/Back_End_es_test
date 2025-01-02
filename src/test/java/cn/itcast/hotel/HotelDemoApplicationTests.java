package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static cn.itcast.hotel.constants.HotelConstants.MAPPING_TEMPLATE;

@SpringBootTest
class HotelDemoApplicationTests {

    private RestHighLevelClient restHighLevelClient;

    @BeforeEach
    public void setUp() {
        restHighLevelClient = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.101.65:9200")
        ));
    }

    @Test
    void contextLoads() {

    }

    @AfterEach
    void testDown() throws IOException {
        this.restHighLevelClient.close();
    }

    //创建索引
    @Test
    void createHotelIndex() throws IOException {
        //创建request对象
        CreateIndexRequest req = new CreateIndexRequest("hotel");
        req.source(MAPPING_TEMPLATE, XContentType.JSON);
        //发送请求
        restHighLevelClient.indices().create(req, RequestOptions.DEFAULT);
    }

    //判断索引库是否存在
    @Test
    void testExistHotelIndex() throws IOException {
        GetIndexRequest request = new GetIndexRequest("hotel");
        boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists?"索引库已经存在":"索引库不存在");
    }






}
