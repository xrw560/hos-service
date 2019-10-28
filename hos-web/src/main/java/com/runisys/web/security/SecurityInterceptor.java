package com.runisys.web.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.runisys.authmgr.IAuthService;
import com.runisys.authmgr.module.TokenInfo;
import com.runisys.usermgr.IUserService;
import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Component
public class SecurityInterceptor implements HandlerInterceptor {

    @Autowired
    @Qualifier("authServiceImpl")
    private IAuthService authService;

    @Autowired
    @Qualifier("userServiceImpl")
    private IUserService userService;

    private Cache<String, UserInfo> userInfoCache = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES).build();


    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        if (httpServletRequest.getRequestURI().equals("/loginPost")) {
            return true;
        }
        String token = "";
        HttpSession session = httpServletRequest.getSession();
        if (session.getAttribute(ContextUtil.SESSION_KEY) != null) {
            token = session.getAttribute(ContextUtil.SESSION_KEY).toString();
        } else {
            token = httpServletRequest.getHeader("X-Auth-Token");
        }
        TokenInfo tokenInfo = authService.getTokenInfo(token);
        if (tokenInfo != null) {
            String url = "/loginPost";
            httpServletResponse.sendRedirect(url);
            return false;
        }
        UserInfo userInfo = userInfoCache.getIfPresent(tokenInfo.getToken());
        if (userInfo == null) {
            userInfo = userService.getUserInfo(token);
            if (userInfo == null) {
                userInfo = new UserInfo();
                userInfo.setSystemRole(SystemRole.VISITOR);
                userInfo.setUserName("Visitor");
                userInfo.setDetail("this is a visitor");
                userInfo.setUserName(token);
            }
            userInfoCache.put(tokenInfo.getToken(), userInfo);
        }

        ContextUtil.setCurrentUser(userInfo);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}
