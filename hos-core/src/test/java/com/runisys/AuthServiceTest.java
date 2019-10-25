package com.runisys;

import com.runisys.authmgr.IAuthService;
import com.runisys.authmgr.module.ServiceAuth;
import com.runisys.authmgr.module.TokenInfo;
import com.runisys.mybatis.BaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Date;
import java.util.List;

public class AuthServiceTest extends BaseTest {

    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Test
    public void addToken() {
        TokenInfo tokenInfo = new TokenInfo("Tom");
        authService.addToken(tokenInfo);
    }


    @Test
    public void getTokenByUser() {
        List<TokenInfo> tokenInfos = authService.getTokenInfos("Tom");
        tokenInfos.forEach(tokenInfo -> {
            System.out.println(tokenInfo);
        });
    }

    @Test
    public void addServiceAuth(){
        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setBucketName("bucket1");
        serviceAuth.setTargetToken("7b14b80cc0d64795a0c03ceb3c9ab37c");
        serviceAuth.setAuthTime(new Date());
        authService.addAuth(serviceAuth);
    }

    @Test
    public void getServiceAuth(){
        ServiceAuth serviceAuth = authService.getServiceAuth("bucket1", "7b14b80cc0d64795a0c03ceb3c9ab37c");
        System.out.println(serviceAuth);
    }


}
