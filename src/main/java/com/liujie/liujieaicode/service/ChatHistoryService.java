package com.liujie.liujieaicode.service;

import com.liujie.liujieaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.liujie.liujieaicode.model.entity.User;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.liujie.liujieaicode.model.entity.ChatHistory;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存用户消息
     *
     * @param appId   应用ID
     * @param userId  用户ID
     * @param message 消息内容
     * @return 记录ID
     */
    Long saveUserMessage(Long appId, Long userId, String message);

    /**
     * 保存AI回复消息
     *
     * @param appId   应用ID
     * @param userId  用户ID
     * @param message AI回复内容
     * @return 记录ID
     */
    Long saveAiMessage(Long appId, Long userId, String message);

    /**
     * 保存AI回复失败的错误信息
     *
     * @param appId        应用ID
     * @param userId       用户ID
     * @param errorMessage 错误信息
     * @return 记录ID
     */
    Long saveAiErrorMessage(Long appId, Long userId, String errorMessage);

    /**
     * 统一游标分页查询对话历史。
     * beforeCreateTime 为 null 时查最新一页，非 null 时查该时间之前的更早记录。
     *
     * @param request 查询请求（含 appId、beforeCreateTime、pageSize 等）
     * @return 对话历史列表（按创建时间降序）
     */
    List<ChatHistory> queryByCursor(ChatHistoryQueryRequest request);

    /**
     * 统一构造游标查询条件。
     * 所有查询接口共用此方法构建 QueryWrapper。
     *
     * @param request 查询请求
     * @return 游标查询条件
     */
    QueryWrapper buildCursorQueryWrapper(ChatHistoryQueryRequest request);

    /**
     * 校验用户是否有权限查看该应用的对话历史
     * （仅应用创建者和管理员可见）
     *
     * @param appId     应用ID
     * @param loginUser 当前登录用户
     */
    void checkChatHistoryPermission(Long appId, User loginUser);

    /**
     * 根据应用ID逻辑删除所有对话历史（关联删除）
     *
     * @param appId 应用ID
     */
    void deleteByAppId(Long appId);
}