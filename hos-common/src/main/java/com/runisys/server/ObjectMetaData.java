package com.runisys.server;

import lombok.Data;

import java.util.Map;

@Data
public class ObjectMetaData {

    private String bucket;
    private String key;
    private String mediaType;
    private long length;
    private long lastModifyTime;
    private Map<String, String> attrs;

    public String getContentEncoding() {
        return attrs != null ? attrs.get("content-encoding") : null;
    }

}
