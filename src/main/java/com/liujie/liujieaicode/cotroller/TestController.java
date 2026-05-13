package com.liujie.liujieaicode.cotroller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/test")
public class TestController {

    @GetMapping("/hello")
    public String test(){
        return "hello world";
    }

}
