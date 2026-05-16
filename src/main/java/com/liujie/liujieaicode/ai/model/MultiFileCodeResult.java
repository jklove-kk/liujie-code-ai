package com.liujie.liujieaicode.ai.model;


import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
@Description("生成多文件代码的返回结果")
public class MultiFileCodeResult {

    @Description("HTML代码")
    private String htmlCode;

    @Description("CSS代码")
    private String cssCode;
    @Description("JS代码")
    private String jsCode;
    @Description("描述")
    private String description;
}
