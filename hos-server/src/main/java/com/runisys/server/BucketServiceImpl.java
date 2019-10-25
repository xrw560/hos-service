package com.runisys.server;

import com.runisys.authmgr.IAuthService;
import com.runisys.authmgr.module.ServiceAuth;
import com.runisys.server.dao.BucketModelMapper;
import com.runisys.usermgr.module.UserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;

@Service("bucketServiceImpl")
@Transactional
public class BucketServiceImpl implements IBucketService {
    @Autowired
    BucketModelMapper bucketModelMapper;
    @Autowired
    @Qualifier("authServiceImpl")
    IAuthService authService;

    @Override
    public boolean addBucket(UserInfo userInfo, String bucketName, String detail) {
        BucketModel bucketModel = new BucketModel(bucketName, userInfo.getUserName(), detail);
        bucketModelMapper.addBucket(bucketModel);
        //todo add auth for bucket and user
        ServiceAuth serviceAuth = new ServiceAuth();
        serviceAuth.setAuthTime(new Date());
        serviceAuth.setBucketName(bucketName);
        serviceAuth.setTargetToken(userInfo.getUserId());
        authService.addAuth(serviceAuth);
        return true;
    }

    @Override
    public boolean deleteBucket(String bucketName) {
        bucketModelMapper.deleteBucket(bucketName);
        //todo delete auth for bucket
        authService.deleteAuthByBucket(bucketName);
        return true;
    }

    @Override
    public boolean updateBucket(String bucketName, String detail) {
        bucketModelMapper.updateBucket(bucketName, detail);
        return true;
    }

    @Override
    public BucketModel getBucketById(String bucketId) {
        return bucketModelMapper.getBucket(bucketId);
    }

    @Override
    public BucketModel getBucketByName(String bucketName) {
        return bucketModelMapper.getBucketByName(bucketName);
    }

    @Override
    public List<BucketModel> getBucketsByCreator(String creator) {
        return bucketModelMapper.getBucketByCreator(creator);
    }

    @Override
    public List<BucketModel> getUserBuckets(String token) {
        return bucketModelMapper.getUserAuthorizedBuckets(token);
    }
}
