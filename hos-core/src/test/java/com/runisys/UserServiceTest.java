package com.runisys;

import com.runisys.mybatis.BaseTest;
import com.runisys.usermgr.IUserService;
import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class UserServiceTest extends BaseTest {
    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Test
    public void addUserTest() {
        UserInfo userInfo = new UserInfo("Tom", "123456", "this is a test user", SystemRole.ADMIN);
        userService.addUser(userInfo);
    }

    @Test
    public void getUserTest() {
        UserInfo userInfo = userService.getUserInfoByName("Tom");
        System.out.println(userInfo);
    }

}
