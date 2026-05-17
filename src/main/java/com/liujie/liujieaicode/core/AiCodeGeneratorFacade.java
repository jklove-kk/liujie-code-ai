package com.liujie.liujieaicode.core;

import com.liujie.liujieaicode.ai.AiCodeGeneratorService;
import com.liujie.liujieaicode.ai.model.HtmlCodeResult;
import com.liujie.liujieaicode.ai.model.MultiFileCodeResult;
import com.liujie.liujieaicode.core.parser.CodeParserExecutor;
import com.liujie.liujieaicode.core.saver.CodeFileSaverExecutor;
import com.liujie.liujieaicode.exception.BusinessException;
import com.liujie.liujieaicode.exception.ErrorCode;
import com.liujie.liujieaicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成外观类，组合生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口：根据类型生成并保存代码(流式)
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateSingleFileCodeStream(userMessage);
                yield processCodeStream(codeStream,codeGenTypeEnum);
            }
            case MULTI_FILE -> {
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield processCodeStream(codeStream,codeGenTypeEnum);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 处理代码流，当流式返回生成代码完成后，再保存代码
     *
     * @param codeStream      代码流
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    private Flux<String> processCodeStream(Flux<String> codeStream,CodeGenTypeEnum codeGenTypeEnum) {
        // 当流式返回生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream
                .doOnNext(chunk -> {
                    // 实时收集代码片段
                    codeBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 流式返回完成后保存代码
                    try {
                        String completeHtmlCode = codeBuilder.toString();
                        Object codeResult = CodeParserExecutor.executeParser(completeHtmlCode, codeGenTypeEnum);
                        // 保存代码到文件
                        File savedDir = CodeFileSaverExecutor.executeSaver(codeResult,codeGenTypeEnum);
                        log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败: {}", e.getMessage());
                    }
                });
    }
}
