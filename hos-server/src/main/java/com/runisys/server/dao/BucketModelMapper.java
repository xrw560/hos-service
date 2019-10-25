package com.runisys.server.dao;

import com.runisys.server.BucketModel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

import java.util.List;

@Mapper
public interface BucketModelMapper {

    void addBucket(@Param("bucket") BucketModel bucketModel);

    void deleteBucket(@Param("bucketName") String bucketName);

    void updateBucket(@Param("bucketName") String bucketName, @Param("detail") String detail);

    @ResultMap("BucketResultMap")
    BucketModel getBucket(@Param("bucketId") String bucketId);

    @ResultMap("BucketResultMap")
    BucketModel getBucketByName(@Param("bucketName") String bucketName);

    @ResultMap("BucketResultMap")
    List<BucketModel> getBucketByCreator(@Param("creator") String creator);

    @ResultMap("BucketResultMap")
    List<BucketModel> getUserAuthorizedBuckets(@Param("token") String token);

}
