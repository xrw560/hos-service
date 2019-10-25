package com.runisys.server;

import com.runisys.usermgr.module.UserInfo;

import java.util.List;

public interface IBucketService {

    public boolean addBucket(UserInfo userInfo, String bucketName, String detail);

    public boolean deleteBucket(String bucketName);

    public boolean updateBucket(String bucketName, String detail);

    public BucketModel getBucketById(String bucketId);

    public BucketModel getBucketByName(String bucketName);

    public List<BucketModel> getBucketsByCreator(String creator);

    public List<BucketModel> getUserBuckets(String token);
}
