package com.liujie.liujieaicode.model.dto.chathistory;

import com.liujie.liujieaicode.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话历史游标查询请求（统一请求类）
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 消息类型：user/ai
     */
    private String messageType;

    /**
     * 应用id
     */
    private Long appId;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 游标：当前页最早一条消息的创建时间（null 表示查最新一页）
     */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime beforeCreateTime;

    private static final long serialVersionUID = 1L;
}