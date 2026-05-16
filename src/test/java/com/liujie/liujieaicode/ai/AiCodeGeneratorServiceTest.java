package com.liujie.liujieaicode.ai;

import com.liujie.liujieaicode.core.AiCodeGeneratorFacade;
import com.liujie.liujieaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;



@SpringBootTest
class AiCodeGeneratorServiceTest {
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;
    @Test
    void generateSingleFileCodeStream() {
        Flux<String> s = aiCodeGeneratorFacade.generateAndSaveCodeStream("生成一个博客网站,不超过30行", CodeGenTypeEnum.HTML);
        List<String> block = s.collectList().block();
        Assertions.assertNotNull(s);
        String join = String.join("", block);
        System.out.println(join);
    }

}