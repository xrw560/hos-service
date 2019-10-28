package com.runisys.web.security;

import com.runisys.usermgr.module.UserInfo;

public class ContextUtil {

    public final static String SESSION_KEY = "USER_TOKEN";
    private static ThreadLocal<UserInfo> userInfoThreadLocal = new ThreadLocal<>();

    public static UserInfo getCurrentUser(){
        return userInfoThreadLocal.get();
    }
    public static void setCurrentUser(UserInfo userInfo){
        userInfoThreadLocal.set(userInfo);
    }
    static void clear(){
        userInfoThreadLocal.remove();
    }

}
