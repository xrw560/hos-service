package com.runisys.web;

import com.runisys.CoreUtil;
import com.runisys.server.IHosStore;
import com.runisys.usermgr.IUserService;
import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppliationInitialization implements ApplicationRunner {

    //创建管理员用户

    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Autowired
    @Qualifier("hosStore")
    IHosStore hosStore;


    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        UserInfo userInfo = userService.getUserInfoByName(CoreUtil.SYSTEM_USER);
        if(userInfo==null){
            UserInfo userInfo1 = new UserInfo(CoreUtil.SYSTEM_USER,"123456","this is a superadmin", SystemRole.SUPERADMIN);
            userService.addUser(userInfo1);
        }

        hosStore.createSeqTable();
    }
}
