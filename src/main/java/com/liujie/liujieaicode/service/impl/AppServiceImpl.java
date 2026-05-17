package com.liujie.liujieaicode.service.impl;

import cn.hutool.core.util.StrUtil;
import com.liujie.liujieaicode.constant.AppConstant;
import com.liujie.liujieaicode.exception.BusinessException;
import com.liujie.liujieaicode.exception.ErrorCode;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.liujie.liujieaicode.model.entity.App;
import com.liujie.liujieaicode.mapper.AppMapper;
import com.liujie.liujieaicode.model.dto.app.AppAddRequest;
import com.liujie.liujieaicode.model.dto.app.AppQueryRequest;
import com.liujie.liujieaicode.model.dto.app.AppUpdateRequest;
import com.liujie.liujieaicode.model.entity.User;
import com.liujie.liujieaicode.service.AppService;
import com.mybatisflex.core.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Override
    public Long addApp(AppAddRequest appAddRequest, User loginUser) {
        if (appAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        String initPrompt = appAddRequest.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        }
        App app = new App();
        app.setAppName(StrUtil.blankToDefault(appAddRequest.getAppName(), "未命名应用"));
        app.setInitPrompt(initPrompt);
        app.setUserId(loginUser.getId());
        app.setPriority(0);
        //设置App文件类型 TODO


        boolean result = this.save(app);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return app.getId();
    }

    @Override
    public Boolean updateMyApp(AppUpdateRequest appUpdateRequest, User loginUser) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null || appUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App oldApp = this.getById(appUpdateRequest.getId());
        checkAppOwner(oldApp, loginUser);
        App app = new App();
        app.setId(appUpdateRequest.getId());
        app.setAppName(appUpdateRequest.getAppName());
        app.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(app);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return true;
    }

    @Override
    public Boolean deleteMyApp(Long id, User loginUser) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = this.getById(id);
        checkAppOwner(app, loginUser);
        boolean result = this.removeById(id);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return true;
    }

    @Override
    /**
     * 检查应用的所有者权限
     * @param app 需要检查的应用对象
     * @param loginUser 当前登录用户
     * @throws BusinessException 当应用不存在、用户未登录或用户不是应用所有者时抛出业务异常
     */
    public void checkAppOwner(App app, User loginUser) {
        // 检查应用是否存在
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 检查用户是否已登录
        if (loginUser == null || loginUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 检查当前用户是否为应用的所有者
        if (!loginUser.getId().equals(app.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public QueryWrapper getFeaturedQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        return QueryWrapper.create()
                .like("appName", appQueryRequest.getAppName())
                //比较精选应用优先级
                .gt("priority", AppConstant.GOOD_APP_PRIORITY)
                .orderBy("priority", false)
                .orderBy("createTime", false);
    }
}
