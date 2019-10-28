package com.runisys.server;

import lombok.Data;

import java.io.File;
import java.util.Map;

@Data
public class PutRequest {

    private String bucket;
    private String key;
    private File file;
    private byte[] content;
    private String contentEncoding;
    private String mediaType;
    private Map<String,String> attrs;

    public PutRequest(String bucket, String key, File file) {
        this.bucket = bucket;
        this.key = key;
        this.file = file;
    }

    public PutRequest(String bucket, String key, File file, String mediaType) {
        this.bucket = bucket;
        this.key = key;
        this.file = file;
        this.mediaType = mediaType;
    }

    public PutRequest(String bucket, String key, byte[] content, String mediaType) {
        this.bucket = bucket;
        this.key = key;
        this.content = content;
        this.mediaType = mediaType;
    }
}
