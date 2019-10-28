package com.runisys.web.security;

import com.runisys.CoreUtil;
import com.runisys.authmgr.IAuthService;
import com.runisys.authmgr.module.ServiceAuth;
import com.runisys.authmgr.module.TokenInfo;
import com.runisys.server.BucketModel;
import com.runisys.server.IBucketService;
import com.runisys.usermgr.IUserService;
import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("DefaultAccessControl")
public class DefaultOperationAccessControl implements IOperationAccessControl {

    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Autowired
    @Qualifier("bucketServiceImpl")
    IBucketService bucketService;


    @Override
    public UserInfo checkLogin(String userName, String password) {
        UserInfo userInfo = userService.getUserInfoByName(userName);
        if (userInfo == null) {
            return null;
        }
        return userInfo.getPassword().equals(CoreUtil.getMd5Password(password)) ? userInfo : null;
    }

    @Override
    public boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2) {
        if (systemRole1.equals(SystemRole.SUPERADMIN)) {
            return true;
        }
        return systemRole1.equals(SystemRole.ADMIN) && systemRole2.equals(SystemRole.USER);
    }

    @Override
    public boolean checkSystemRole(SystemRole systemRole, String userId) {
        if (systemRole.equals(SystemRole.SUPERADMIN)) {
            return true;
        }
        UserInfo userInfo = userService.getUserInfo(userId);
        return systemRole.equals(SystemRole.ADMIN) && userInfo.getSystemRole().equals(SystemRole.USER);
    }

    @Override
    public boolean checkTokenOwner(String userName, String token) {
        TokenInfo tokenInfo = authService.getTokenInfo(token);
        return tokenInfo.getCreator().equals(userName);
    }

    @Override
    public boolean checkBucketOwner(String userName, String bucket) {
        BucketModel bucketModel = bucketService.getBucketByName(bucket);
        return bucketModel.getCreator().equals(userName);
    }

    @Override
    public boolean checkPermission(String token, String bucket) {
        if (authService.checkToken(token)) {
            ServiceAuth serviceAuth = authService.getServiceAuth(bucket, token);
            if (serviceAuth != null) {
                return true;
            }
        }
        return false;
    }
}
