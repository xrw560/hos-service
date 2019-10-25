package com.runisys.authmgr.module;

import com.runisys.CoreUtil;
import lombok.Data;

import java.util.Date;

@Data
public class TokenInfo {
    private String token;
    private int expireTime;
    private Date refreshTime;
    private Date createTime;
    private boolean active;
    private String creator;

    public TokenInfo() {
    }

    public TokenInfo(String creator) {
        this.token = CoreUtil.getUUIDStr();
        this.expireTime = 7;
        this.creator = creator;
        Date now = new Date();
        this.refreshTime = now;
        this.createTime = now;
        this.active = true;
    }
}
