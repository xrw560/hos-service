package com.runisys.authmgr.module;

import lombok.Data;

import java.util.Date;

@Data
public class ServiceAuth {
    private String bucketName;
    private String targetToken;
    private Date authTime;
}
