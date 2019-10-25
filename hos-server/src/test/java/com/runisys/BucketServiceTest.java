package com.runisys;

import com.runisys.mybatis.BaseTest;
import com.runisys.server.BucketModel;
import com.runisys.server.IBucketService;
import com.runisys.usermgr.IUserService;
import com.runisys.usermgr.module.UserInfo;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class BucketServiceTest extends BaseTest {

    @Autowired
    @Qualifier("bucketServiceImpl")
    IBucketService bucketService;

    @Autowired
    @Qualifier("userServiceImpl")
    IUserService userService;

    @Test
    public void addBucket() {
        UserInfo userInfo = userService.getUserInfoByName("Tom");
        bucketService.addBucket(userInfo, "bucket1", "this is a test bucket");
    }


    @Test
    public void getBucket(){
        BucketModel bucketModel = bucketService.getBucketByName("bucket1");
        System.out.printf(bucketModel.toString());
    }
}
