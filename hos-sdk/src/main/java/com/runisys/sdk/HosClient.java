package com.runisys.sdk;

import com.runisys.server.BucketModel;
import com.runisys.server.HosObjectSummary;
import com.runisys.server.PutRequest;

import java.io.IOException;
import java.util.List;

public interface HosClient {
    public void createBucket(String bucketName, String detail) throws IOException;

    public void deleteBucket(String bucketName) throws IOException;

    public List<BucketModel> listBucket() throws IOException;

    public void putObject(PutRequest putRequest) throws IOException;

    public void putObject(String bucket, String key) throws IOException;

    public void putObject(String bucket, String key, byte[] content, String mediaType) throws IOException;

    public void deleteObject(String bucket, String key) throws IOException;

    public HosObjectSummary getObjectSummary(String bucket, String key) throws IOException;

}
