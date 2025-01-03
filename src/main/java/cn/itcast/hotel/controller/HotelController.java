package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hotel")
public class HotelController {
    @Autowired
    private IHotelService hotelService;

    //搜索酒店数据
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams params) throws IOException {
        return hotelService.search(params);
    }

    @PostMapping("filters")
    public Map<String, List<String>> getFilter(@RequestBody RequestParams params) throws IOException {
        return hotelService.getFilters(params);
    }

    @GetMapping("suggestion")
    public List<String> getSuggestions(@RequestParam("key") String prefix) {
        return hotelService.getSuggestions(prefix);
    }
}
