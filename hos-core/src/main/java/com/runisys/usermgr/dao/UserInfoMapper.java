package com.runisys.usermgr.dao;

import com.runisys.usermgr.module.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;

@Mapper
public interface UserInfoMapper {

    void addUser(@Param("userInfo") UserInfo userInfo);

    int deleteUser(@Param("userId") String userId);

    int updateUserInfo(@Param("userId") String userId, @Param("password") String password, @Param("detail") String detail);

    @ResultMap("UserInfoResultMap")
    UserInfo getUserInfo(@Param("userId") String userId);

    @ResultMap("UserInfoResultMap")
    UserInfo getUserInfoByName(@Param("userName") String userName);

    @ResultMap("UserInfoResultMap")
    UserInfo checkPassword(@Param("userName") String userName, @Param("password") String password);


}
