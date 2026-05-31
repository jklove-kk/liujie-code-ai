package com.liujie.liujieaicode.service;

import com.liujie.liujieaicode.model.dto.app.AppAddRequest;
import com.liujie.liujieaicode.model.dto.app.AppQueryRequest;
import com.liujie.liujieaicode.model.dto.app.AppUpdateRequest;
import com.mybatisflex.core.service.IService;
import com.liujie.liujieaicode.model.entity.App;
import com.liujie.liujieaicode.model.entity.User;
import com.mybatisflex.core.query.QueryWrapper;
import reactor.core.publisher.Flux;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/jklove-kk/liujie-code-ai">Jklove</a>
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddRequest 创建请求
     * @param loginUser     当前登录用户
     * @return 应用 id
     */
    Long addApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 用户更新自己的应用
     *
     * @param appUpdateRequest 更新请求
     * @param loginUser        当前登录用户
     * @return 是否成功
     */
    Boolean updateMyApp(AppUpdateRequest appUpdateRequest, User loginUser);

    /**
     * 用户删除自己的应用
     *
     * @param id        应用 id
     * @param loginUser 当前登录用户
     * @return 是否成功
     */
    Boolean deleteMyApp(Long id, User loginUser);

    /**
     * 校验是否为应用创建者
     *
     * @param app       应用
     * @param loginUser 当前登录用户
     */
    void checkAppOwner(App app, User loginUser);

    /**
     * 获取应用查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取精选应用查询条件
     *
     * @param appQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper getFeaturedQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 通过聊天生成代码
     *
     * @param appId
     * @param message
     * @param loginUser
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);


    String deployApp(Long appId, User loginUser);
}
