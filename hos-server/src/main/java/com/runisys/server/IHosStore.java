package com.runisys.server;

import org.apache.xerces.xs.datatypes.ObjectList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface IHosStore {

    public void createBucketStore(String bucket) throws IOException;

    public void deleteBucketStore(String bucket) throws IOException;

    public void createSeqTable() throws IOException;

    public void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> properties) throws Exception;

    public HosObjectSummary getSummary(String bucket, String key) throws IOException;

    public List<HosObjectSummary> list(String bucket, String startKey, String endKey) throws IOException;

    public ObjectListResult listDir(String bucket, String dir, String start, int maxCount) throws IOException;

    public ObjectListResult listByPrefix(String bucket, String dir, String start, String prefix, int maxCount) throws IOException;

    public HosObject getObject(String bucket, String key) throws IOException;

    public void deleteObject(String bucket, String key) throws Exception;

}
