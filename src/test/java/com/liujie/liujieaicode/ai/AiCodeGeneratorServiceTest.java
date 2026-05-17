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
        Flux<String> s = aiCodeGeneratorFacade.generateAndSaveCodeStream("生成一个网站。硬性要求：\n" +
                "1. 最终只输出一个 html 代码块，不要任何解释。\n" +
                "2. HTML 代码总行数必须 <= 20 行。\n" +
                "3. 可以牺牲美观、注释和复杂交互，但绝不能超过 20 行。", CodeGenTypeEnum.HTML);
        List<String> block = s.collectList().block();
        Assertions.assertNotNull(s);
        String join = String.join("", block);
        System.out.println(join);
    }

}