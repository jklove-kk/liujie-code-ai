package com.liujie.liujieaicode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.liujie.liujieaicode.exception.BusinessException;
import com.liujie.liujieaicode.exception.ErrorCode;
import com.liujie.liujieaicode.exception.ThrowUtils;
import com.liujie.liujieaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.liujie.liujieaicode.model.entity.App;
import com.liujie.liujieaicode.model.entity.User;
import com.liujie.liujieaicode.model.enums.MessageTypeEnum;
import com.liujie.liujieaicode.constant.UserConstant;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.liujie.liujieaicode.model.entity.ChatHistory;
import com.liujie.liujieaicode.mapper.ChatHistoryMapper;
import com.liujie.liujieaicode.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.annotation.Resource;
import com.liujie.liujieaicode.mapper.AppMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    @Resource
    private AppMapper appMapper;

    @Override
    public Long saveUserMessage(Long appId, Long userId, String message) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息不能为空");
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(message)
                .messageType(MessageTypeEnum.USER.getValue())
                .build();
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存用户消息失败");
        return chatHistory.getId();
    }

    @Override
    public Long saveAiMessage(Long appId, Long userId, String message) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message(StrUtil.blankToDefault(message, ""))
                .messageType(MessageTypeEnum.AI.getValue())
                .build();
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存AI消息失败");
        return chatHistory.getId();
    }

    @Override
    public Long saveAiErrorMessage(Long appId, Long userId, String errorMessage) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(errorMessage), ErrorCode.PARAMS_ERROR, "错误信息不能为空");
        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .message("")
                .errorMessage(errorMessage)
                .messageType(MessageTypeEnum.AI.getValue())
                .build();
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存AI错误信息失败");
        return chatHistory.getId();
    }

    @Override
    public List<ChatHistory> queryByCursor(ChatHistoryQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        if (request.getPageSize() <= 0) {
            request.setPageSize(10);
        }
        ThrowUtils.throwIf(request.getPageSize() > 50, ErrorCode.PARAMS_ERROR, "每页最多查询 50 条");
        QueryWrapper queryWrapper = buildCursorQueryWrapper(request);
        return this.mapper.selectListByQuery(queryWrapper);
    }

    @Override
    public QueryWrapper buildCursorQueryWrapper(ChatHistoryQueryRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;

        return QueryWrapper.create()
                .eq("id", request.getId())
                .eq("messageType", request.getMessageType())
                .eq("appId", request.getAppId())
                .eq("userId", request.getUserId())
                .lt("createTime", request.getBeforeCreateTime())
                .orderBy("createTime", false)
                .limit(pageSize);
    }

    @Override
    public void checkChatHistoryPermission(Long appId, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 管理员直接放行
        if (UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            return;
        }
        // 检查是否为应用创建者
        App app = appMapper.selectOneById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!loginUser.getId().equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用的对话历史");
        }
    }

    @Override
    public void deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        this.mapper.deleteByAppId(appId);
    }

    @Override
    public int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 直接构造查询条件，起始点为 1 而不是 0，用于排除最新的用户消息
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq(ChatHistory::getAppId, appId)
                    .orderBy(ChatHistory::getCreateTime, false)
                    .limit(1, maxCount);
            List<ChatHistory> historyList = this.list(queryWrapper);
            if (CollUtil.isEmpty(historyList)) {
                return 0;
            }
            // 反转列表，确保按时间正序（老的在前，新的在后）
            historyList = historyList.reversed();
            // 按时间顺序添加到记忆中
            int loadedCount = 0;
            // 先清理历史缓存，防止重复加载
            chatMemory.clear();
            for (ChatHistory history : historyList) {
                if (MessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                } else if (MessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
            }
            log.info("成功为 appId: {} 加载了 {} 条历史对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载历史对话失败，appId: {}, error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有历史上下文
            return 0;
        }
    }

}