package com.liujie.liujieaicode.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.liujie.liujieaicode.constant.AppConstant;
import com.liujie.liujieaicode.core.AiCodeGeneratorFacade;
import com.liujie.liujieaicode.exception.BusinessException;
import com.liujie.liujieaicode.exception.ErrorCode;
import com.liujie.liujieaicode.exception.ThrowUtils;
import com.liujie.liujieaicode.model.enums.CodeGenTypeEnum;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.liujie.liujieaicode.model.entity.App;
import com.liujie.liujieaicode.mapper.AppMapper;
import com.liujie.liujieaicode.model.dto.app.AppAddRequest;
import com.liujie.liujieaicode.model.dto.app.AppQueryRequest;
import com.liujie.liujieaicode.model.dto.app.AppUpdateRequest;
import com.liujie.liujieaicode.model.entity.User;
import com.liujie.liujieaicode.service.AppService;
import com.liujie.liujieaicode.service.ChatHistoryService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

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
        app.setCodeGenType(appUpdateRequest.getCodeGenType());
        app.setEditTime(LocalDateTime.now());
        boolean result = this.updateById(app);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteMyApp(Long id, User loginUser) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        App app = this.getById(id);
        checkAppOwner(app, loginUser);
        // 关联删除该应用的所有对话历史
        chatHistoryService.deleteByAppId(id);
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


    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 保存用户消息到对话历史
        Long userMessageId = chatHistoryService.saveUserMessage(appId, loginUser.getId(), message);
        // 5. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        // 6. 调用 AI 生成代码（流式），并保存AI回复结果
        StringBuilder aiResponseBuilder = new StringBuilder();
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, appId)
                .doOnNext(chunk -> {
                    // 收集完整的AI回复内容
                    aiResponseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // AI 回复成功，保存完整消息
                    String aiResponse = aiResponseBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.saveAiMessage(appId, loginUser.getId(), aiResponse);
                    }
                })
                .doOnError(error -> {
                    // AI 回复失败，记录错误信息
                    chatHistoryService.saveAiErrorMessage(appId, loginUser.getId(),
                            "AI回复失败: " + error.getMessage());
                });
    }


    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 检查是否已有 deployKey
        String deployKey = app.getDeployKey();
        // 没有则生成 6 位 deployKey（大小写字母 + 数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6. 检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }
        // 7. 复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 9. 返回可访问的 URL
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }


}
