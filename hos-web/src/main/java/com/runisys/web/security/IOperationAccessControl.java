package com.runisys.web.security;

import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;

public interface IOperationAccessControl {

    public UserInfo checkLogin(String userName, String password);

    /**
     * systemRole1对systemRole2是否有操作权限
     *
     * @param systemRole1
     * @param systemRole2
     * @return
     */
    public boolean checkSystemRole(SystemRole systemRole1, SystemRole systemRole2);

    /**
     * systemRole是否对userId有操作权限
     *
     * @param systemRole
     * @param userId
     * @return
     */
    public boolean checkSystemRole(SystemRole systemRole, String userId);

    /**
     * userName是否为token的创建者
     *
     * @param userName
     * @param token
     * @return
     */
    public boolean checkTokenOwner(String userName, String token);

    /**
     * userName是否为bucket的创建者
     *
     * @param userName
     * @param bucket
     * @return
     */
    public boolean checkBucketOwner(String userName, String bucket);

    /**
     * token是否对bucket有操作权限
     *
     * @param token
     * @param bucket
     * @return
     */
    public boolean checkPermission(String token, String bucket);

}
