package com.liujie.liujieaicode.cotroller;

import cn.hutool.core.bean.BeanUtil;
import com.liujie.liujieaicode.annotation.AuthCheck;
import com.liujie.liujieaicode.common.BaseResponse;
import com.liujie.liujieaicode.common.DeleteRequest;
import com.liujie.liujieaicode.common.ResultUtils;
import com.liujie.liujieaicode.constant.UserConstant;
import com.liujie.liujieaicode.exception.BusinessException;
import com.liujie.liujieaicode.exception.ErrorCode;
import com.liujie.liujieaicode.exception.ThrowUtils;
import com.liujie.liujieaicode.model.dto.app.AppAddRequest;
import com.liujie.liujieaicode.model.dto.app.AppAdminUpdateRequest;
import com.liujie.liujieaicode.model.dto.app.AppQueryRequest;
import com.liujie.liujieaicode.model.dto.app.AppUpdateRequest;
import com.liujie.liujieaicode.model.entity.App;
import com.liujie.liujieaicode.model.entity.User;
import com.liujie.liujieaicode.service.AppService;
import com.liujie.liujieaicode.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    /**
     * 用户创建应用
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long appId = appService.addApp(appAddRequest, loginUser);
        return ResultUtils.success(appId);
    }

    /**
     * 用户根据 id 修改自己的应用（目前只支持修改应用名称）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateMyApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = appService.updateMyApp(appUpdateRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户根据 id 删除自己的应用
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMyApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Boolean result = appService.deleteMyApp(deleteRequest.getId(), loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 用户根据 id 查看自己的应用详情
     */
    @GetMapping("/get")
    public BaseResponse<App> getMyAppById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        App app = appService.getById(id);
        //查询用户信息并返回 TODO
        appService.checkAppOwner(app, loginUser);
        return ResultUtils.success(app);
    }

    /**
     * 用户分页查询自己的应用列表
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<App>> listMyAppByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                   HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 条");
        User loginUser = userService.getLoginUser(request);
        appQueryRequest.setUserId(loginUser.getId());
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    /**
     * 用户分页查询精选应用列表
     */
    @PostMapping("/good/list/page")
    public BaseResponse<Page<App>> listGoodAppByPage(@RequestBody AppQueryRequest appQueryRequest,
                                                     HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 条");
        userService.getLoginUser(request);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), appService.getFeaturedQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    /**
     * 管理员根据 id 删除任意应用
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员根据 id 更新任意应用
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null
                || appAdminUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Integer priority = appAdminUpdateRequest.getPriority();
        ThrowUtils.throwIf(priority != null && priority < 0, ErrorCode.PARAMS_ERROR, "优先级不能小于 0");
        App oldApp = appService.getById(appAdminUpdateRequest.getId());
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页查询应用列表
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<App>> listAppByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), appService.getQueryWrapper(appQueryRequest));
        return ResultUtils.success(appPage);
    }

    /**
     * 管理员根据 id 查看应用详情
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> getAppByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

}
