package com.liujie.liujieaicode.mapper;

import com.mybatisflex.core.BaseMapper;
import com.liujie.liujieaicode.model.entity.ChatHistory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 对话历史 映射层。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 根据应用ID逻辑删除所有对话历史（关联删除）
     *
     * @param appId 应用ID
     * @return 影响行数
     */
    int deleteByAppId(@Param("appId") Long appId);
}