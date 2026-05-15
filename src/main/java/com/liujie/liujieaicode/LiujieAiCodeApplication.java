package com.liujie.liujieaicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.liujie.liujieaicode.mapper")
public class LiujieAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiujieAiCodeApplication.class, args);
    }

}
