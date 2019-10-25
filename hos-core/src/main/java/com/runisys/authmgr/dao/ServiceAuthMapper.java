package com.runisys.authmgr.dao;

import com.runisys.authmgr.module.ServiceAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;


@Mapper
public interface ServiceAuthMapper {

    public void addAuth(@Param("auth") ServiceAuth auth);

    public void deleteAuth(@Param("bucket") String bucket, @Param("token") String token);

    public void deleteAuthByToken(@Param("token") String token);

    public void deleteAuthByBucket(@Param("bucket") String bucket);

    @ResultMap("ServiceAuthResultMap")
    public ServiceAuth getAuth(@Param("bucket") String bucket, @Param("token") String token);


}
