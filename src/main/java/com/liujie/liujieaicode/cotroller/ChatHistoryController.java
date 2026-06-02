package com.liujie.liujieaicode.cotroller;

import com.liujie.liujieaicode.annotation.AuthCheck;
import com.liujie.liujieaicode.common.BaseResponse;
import com.liujie.liujieaicode.common.DeleteRequest;
import com.liujie.liujieaicode.common.ResultUtils;
import com.liujie.liujieaicode.constant.UserConstant;
import com.liujie.liujieaicode.exception.BusinessException;
import com.liujie.liujieaicode.exception.ErrorCode;
import com.liujie.liujieaicode.exception.ThrowUtils;
import com.liujie.liujieaicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.liujie.liujieaicode.model.entity.User;
import com.liujie.liujieaicode.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import com.liujie.liujieaicode.model.entity.ChatHistory;
import com.liujie.liujieaicode.service.ChatHistoryService;

import java.util.List;

/**
 * 对话历史 控制层
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    // ==================== 用户端游标查询接口 ====================

    /**
     * 获取应用的最新对话历史（游标分页，默认10条，按时间降序）
     * 进入应用页面时调用，仅应用创建者和管理员可见
     *
     * @param appId   应用ID
     * @param limit   条数（默认10）
     * @param request 请求对象
     * @return 对话历史列表
     */
    @GetMapping("/app/{appId}")
    public BaseResponse<List<ChatHistory>> getLatestByAppId(@PathVariable Long appId,
                                                             @RequestParam(defaultValue = "10") int limit,
                                                             HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        User loginUser = userService.getLoginUser(request);
        chatHistoryService.checkChatHistoryPermission(appId, loginUser);
        ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
        queryRequest.setAppId(appId);
        queryRequest.setPageSize(limit);
        List<ChatHistory> list = chatHistoryService.queryByCursor(queryRequest);
        return ResultUtils.success(list);
    }

    /**
     * 加载更早的对话历史（游标分页，向前加载更多）
     * 传入当前页最早一条消息的 createTime，加载更早的 N 条历史记录
     *
     * @param appId   应用ID
     * @param request 请求对象（beforeCreateTime 和 pageSize 通过 ChatHistoryQueryRequest 传入）
     * @return 更早的对话历史列表
     */
    @GetMapping("/app/{appId}/more")
    public BaseResponse<List<ChatHistory>> getMoreByAppId(@PathVariable Long appId,
                                                           ChatHistoryQueryRequest request,
                                                           HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(request.getBeforeCreateTime() == null, ErrorCode.PARAMS_ERROR, "游标时间无效");
        User loginUser = userService.getLoginUser(httpRequest);
        chatHistoryService.checkChatHistoryPermission(appId, loginUser);
        request.setAppId(appId);
        List<ChatHistory> list = chatHistoryService.queryByCursor(request);
        return ResultUtils.success(list);
    }


    // ==================== 管理员接口 ====================

    /**
     * 管理员游标查询所有应用的对话历史（按时间降序，便于内容监管）
     *
     * @param queryRequest 查询请求（支持 appId、userId、messageType、beforeCreateTime 筛选）
     * @return 对话历史列表
     */
    @PostMapping("/admin/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<ChatHistory>> listChatHistoryByAdmin(@RequestBody ChatHistoryQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        List<ChatHistory> list = chatHistoryService.queryByCursor(queryRequest);
        return ResultUtils.success(list);
    }

    /**
     * 管理员根据ID删除对话历史
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteChatHistoryByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = chatHistoryService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员根据应用ID删除该应用的所有对话历史
     */
    @PostMapping("/admin/deleteByAppId")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteByAppId(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        chatHistoryService.deleteByAppId(deleteRequest.getId());
        return ResultUtils.success(true);
    }
}