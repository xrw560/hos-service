package com.runisys.usermgr;

import com.runisys.usermgr.module.UserInfo;

public interface IUserService {
    public boolean addUser(UserInfo userInfo);
    public boolean updateUserInfo(String userId, String password, String detail);
    public boolean deleteUser(String userId);
    public UserInfo getUserInfo(String userId);
    public UserInfo getUserInfoByName(String userName);
    public UserInfo checkPassword(String userName,String password);
}
