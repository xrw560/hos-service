package com.runisys.web.rest;

import com.google.common.base.Strings;
import com.runisys.ErrorCodes;
import com.runisys.usermgr.module.UserInfo;
import com.runisys.web.security.ContextUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Hos服务登录验证
 */
@Controller
public class LoginController extends BaseController {

    @RequestMapping("/loginPost")
    @ResponseBody
    public Object loginPost(String userName, String password, HttpSession session) {
        if (Strings.isNullOrEmpty(userName) || Strings.isNullOrEmpty(password)) {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
        }
        UserInfo userInfo = operationAccessControl.checkLogin(userName, password);
        if (userInfo != null) {
            session.setAttribute(ContextUtil.SESSION_KEY, userInfo.getUserId());
            return getResult("success");
        } else {
            return getError(ErrorCodes.ERROR_PERMISSION_DENIED, "error");
        }
    }

    @RequestMapping("/logout")
    @ResponseBody
    public Object logout(HttpSession session) {
        session.removeAttribute(ContextUtil.SESSION_KEY);
        return getResult("success");
    }

}
