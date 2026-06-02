package com.liujie.liujieaicode.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.liujie.liujieaicode.model.entity.ChatHistory;
import com.liujie.liujieaicode.mapper.ChatHistoryMapper;
import com.liujie.liujieaicode.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
