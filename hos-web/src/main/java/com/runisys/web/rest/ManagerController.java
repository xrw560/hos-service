package com.runisys.web.rest;

import com.runisys.ErrorCodes;
import com.runisys.authmgr.IAuthService;
import com.runisys.authmgr.module.ServiceAuth;
import com.runisys.authmgr.module.TokenInfo;
import com.runisys.server.IBucketService;
import com.runisys.usermgr.IUserService;
import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;
import com.runisys.web.security.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

/**
 * Hos服务用户和权限管理接口
 */
@RestController
@RequestMapping("hos/v1/sys")
public class ManagerController extends BaseController {


    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Autowired
    @Qualifier("bucketServiceImpl")
    IBucketService bucketService;

    //添加用户
    @RequestMapping(value = "user", method = RequestMethod.POST)
    public Object createUser(@RequestParam("username") String userName,
                             @RequestParam("password") String password,
                             @RequestParam(name = "detail", required = false, defaultValue = "") String detail,
                             @RequestParam(name = "role", required = false, defaultValue = "USER") String role) {
        UserInfo userInfo = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkSystemRole(userInfo.getSystemRole(), SystemRole.valueOf(role))) {
            UserInfo userInfo1 = new UserInfo(userName, password, detail, SystemRole.valueOf(role));
            userService.addUser(userInfo1);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
    }

    //删除用户
    @RequestMapping(value = "user", method = RequestMethod.DELETE)
    public Object deleteUser(@RequestParam("userId") String userId) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkSystemRole(currentUser.getSystemRole(), userId)) {
            userService.deleteUser(userId);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
    }

    //添加token
    @RequestMapping(value = "token", method = RequestMethod.POST)
    public Object createToken(@RequestParam(name = "expireTime", required = false, defaultValue = "7") String expireTime,
                              @RequestParam(name = "isActive", required = false, defaultValue = "true") String isActive) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!currentUser.getSystemRole().equals(SystemRole.VISITOR)) {
            TokenInfo tokenInfo = new TokenInfo(currentUser.getUserName());
            tokenInfo.setExpireTime(Integer.parseInt(expireTime));
            tokenInfo.setActive(Boolean.parseBoolean(isActive));
            authService.addToken(tokenInfo);
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
    }


    //删除token
    @RequestMapping(value = "token", method = RequestMethod.DELETE)
    public Object deleteToken(@RequestParam("token") String token) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)) {
            authService.deleteToken(token);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
    }

    //授权
    @RequestMapping(value = "auth", method = RequestMethod.POST)
    public Object createAuth(@RequestBody ServiceAuth serviceAuth) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), serviceAuth.getTargetToken())
                && operationAccessControl.checkBucketOwner(currentUser.getUserName(), serviceAuth.getBucketName())) {
            authService.addAuth(serviceAuth);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
    }

    //取消授权
    @RequestMapping(value = "auth", method = RequestMethod.DELETE)
    public Object deleteAuth(@RequestParam("bucket") String bucket,
                             @RequestParam("token") String token) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkTokenOwner(currentUser.getUserName(), token)
                && operationAccessControl.checkBucketOwner(currentUser.getUserName(), bucket)) {
            authService.deleteAuth(bucket, token);
            return getResult("success");
        }
        return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
    }
}
