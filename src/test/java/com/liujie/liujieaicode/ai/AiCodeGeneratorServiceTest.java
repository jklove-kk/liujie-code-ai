package com.liujie.liujieaicode.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class AiCodeGeneratorServiceTest {


    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Test
    void generateSingleFileCode() {
        String s = aiCodeGeneratorService.generateSingleFileCode("生成一个博客网站,不超过30行");
        Assertions.assertNotNull(s);
    }

    @Test
    void generateMultiFileCode() {
        String s = aiCodeGeneratorService.generateMultiFileCode("生成一个博客网站，不超过50行");
        Assertions.assertNotNull(s);
    }
}