package com.runisys.authmgr;

import com.runisys.authmgr.module.ServiceAuth;
import com.runisys.authmgr.module.TokenInfo;

import java.util.List;

public interface IAuthService {

    public boolean addAuth(ServiceAuth serviceAuth);

    public boolean deleteAuth(String bucketName, String token);

    public boolean deleteAuthByToken(String token);

    public boolean deleteAuthByBucket(String bucket);

    public ServiceAuth getServiceAuth(String bucket, String token);

    public boolean addToken(TokenInfo tokenInfo);

    public boolean deleteToken(String token);

    public boolean updateToken(String token, int expireTime, boolean isActive);

    public boolean refreshToken(String token);

    /**
     * token是否有效
     *
     * @param token
     * @return
     */
    public boolean checkToken(String token);

    public TokenInfo getTokenInfo(String token);

    public List<TokenInfo> getTokenInfos(String creator);

}
