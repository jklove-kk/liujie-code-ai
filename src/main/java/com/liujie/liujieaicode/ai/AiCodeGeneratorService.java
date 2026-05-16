package com.liujie.liujieaicode.ai;

import dev.langchain4j.service.SystemMessage;

public interface AiCodeGeneratorService {




    @SystemMessage(fromResource = "prompt/single-file-html-prompt.txt")
    String generateSingleFileCode(String userMessage);



    @SystemMessage(fromResource = "prompt/multi-file-html-prompt.txt")
    String generateMultiFileCode(String userMessage);


}
