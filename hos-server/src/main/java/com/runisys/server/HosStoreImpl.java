package com.runisys.server;

import com.google.common.base.Strings;
import com.runisys.ErrorCodes;
import com.runisys.server.util.JsonUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.io.ByteBufferInputStream;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.log4j.Logger;
import org.apache.xerces.xs.datatypes.ObjectList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class HosStoreImpl implements IHosStore {

    private static Logger logger = Logger.getLogger(HosStoreImpl.class);
    private Connection connection = null;
    private IHdfsService fileStore;
    private String zkUrls;
    private CuratorFramework zkClient;

    public HosStoreImpl(Connection connection, IHdfsService fileStore, String zkUrls) {
        this.connection = connection;
        this.fileStore = fileStore;
        this.zkUrls = zkUrls;
        zkClient = CuratorFrameworkFactory.newClient(zkUrls, new ExponentialBackoffRetry(20, 5));
        this.zkClient.start();
    }

    @Override
    public void createBucketStore(String bucket) throws IOException {
        //1. 创建目录表
        HBaseServiceImpl.createTable(connection, HosUtil.getDirTableName(bucket), HosUtil.getDirColumnFamily(), null);
        //2. 创建文件表
        HBaseServiceImpl.createTable(connection, HosUtil.getObjTableName(bucket), HosUtil.getObjColumnFamily(), HosUtil.OBJ_REGIONS);

        //3. 将其添加到seq表
        Put put = new Put(bucket.getBytes());
        put.addColumn(HosUtil.BUCKET_DIR_SEQ_CF_BYTES, HosUtil.BUCKET_DIR_SEQ_QUALIFIER, Bytes.toBytes(0L));
        HBaseServiceImpl.putRow(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, put);
        //4. 创建HDFS目录
        fileStore.mkDir(HosUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void deleteBucketStore(String bucket) throws IOException {
        //删除目录表和文件表
        HBaseServiceImpl.deleteTable(connection, HosUtil.getDirTableName(bucket));
        HBaseServiceImpl.deleteTable(connection, HosUtil.getObjTableName(bucket));
        //删除seq表中的记录
        HBaseServiceImpl.deleteRow(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, bucket);
        //删除HDFS上的目录
        fileStore.deleteDir(HosUtil.FILE_STORE_ROOT + "/" + bucket);
    }

    @Override
    public void createSeqTable() throws IOException {
        HBaseServiceImpl.createTable(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, new String[]{HosUtil.BUCKET_DIR_SEQ_CF}, null);
    }

    @Override
    public void put(String bucket, String key, ByteBuffer content, long length, String mediaType, Map<String, String> properties) throws Exception {
        InterProcessMutex lock = null;
        //判断是否是创建目录
        if (key.endsWith("/")) {
            putDir(bucket, key);
            return;
        }
        //上传文件到文件表(去目录表获取seqId)

        //获取seqId
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String hash = null;
        while (hash == null) {
            if (!dirExist(bucket, dir)) {
                hash = putDir(bucket, dir);
            } else {
                hash = getDirSeqId(bucket, dir);
            }
        }
        //获取锁
        String lockey = key.replace("/", "_");
        lock = new InterProcessMutex(zkClient, "/hos/" + bucket + "/" + lockey);
        lock.acquire();
        //上传文件
        String fileKey = hash + "_" + key.substring(key.lastIndexOf("/") + 1);
        Put contentPut = new Put(fileKey.getBytes());
        if (!Strings.isNullOrEmpty(mediaType)) {
            contentPut.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_MEDIATYPE_QUALIFIER, mediaType.getBytes());
        }
        //todo add props length

        //判断文件的大小，如果小于20M, 存储到HBase，否则存储到HDFS
        if (length <= HosUtil.FILE_STORE_THRESHOLD) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(HosUtil.OBJ_CONT_QUALIFIER);
            contentPut.addColumn(HosUtil.OBJ_CONT_CF_BYTES, byteBuffer, System.currentTimeMillis(), content);
            byteBuffer.clear();
        } else {
            String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + hash;
            String name = key.substring(key.lastIndexOf("/") + 1);
            InputStream inputStream = new ByteBufferInputStream(content);
            fileStore.saveFile(fileDir, name, inputStream, length, (short) 1);
        }
        HBaseServiceImpl.putRow(connection, HosUtil.getObjTableName(bucket), contentPut);
        //释放锁
        if (lock != null) {
            lock.release();
        }
    }

    @Override
    public HosObjectSummary getSummary(String bucket, String key) throws IOException {

        //判断是否为文件夹
        if (key.endsWith("/")) {
            Result result = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
            if (!result.isEmpty()) {
                //读取文件夹的基础属性，转换为HosObjectSummary
                return this.dirObjectToSummary(result, bucket, key);
            }
            return null;
        }
        //获取文件的基本属性
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String seq = getDirSeqId(bucket, dir);
        if (seq == null) {
            return null;
        }
        String objKey = seq + "_" + key.substring(key.lastIndexOf("/") + 1);
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey);
        if (result.isEmpty()) {
            return null;
        }
        return this.resultToObjectSummary(result, bucket, dir);
    }

    /**
     * 文件表数据读取
     *
     * @param result
     * @param bucket
     * @param dir
     * @return
     */
    private HosObjectSummary resultToObjectSummary(Result result, String bucket, String dir) {
        HosObjectSummary summary = new HosObjectSummary();
        long timestamp = result.rawCells()[0].getTimestamp();
        summary.setLastModifyTime(timestamp);
        String id = new String(result.getRow());
        summary.setId(id);
        String name = id.split("_", 2)[1];
        summary.setName(name);
        summary.setKey(dir + name);
        summary.setBucket(bucket);
        summary.setMediaType(Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_MEDIATYPE_QUALIFIER)));
        //todo length attr
        return summary;
    }

    /**
     * 目录表数据读取
     *
     * @param result
     * @param bucket
     * @param dir
     * @return
     */
    private HosObjectSummary dirObjectToSummary(Result result, String bucket, String dir) {
        HosObjectSummary summary = new HosObjectSummary();
        summary.setId(Bytes.toString(result.getRow()));
        summary.setAttrs(new HashMap<>(0));
        summary.setBucket(bucket);
        summary.setLastModifyTime(result.rawCells()[0].getTimestamp());
        summary.setLength(0L);
        summary.setMediaType("");
        if (dir.length() > 1) {
            summary.setName(dir.substring(dir.lastIndexOf("/") + 1));
        } else {
            summary.setName("");
        }
        return summary;
    }

    @Override
    public List<HosObjectSummary> list(String bucket, String startKey, String endKey) throws IOException {
        return null;
    }

    @Override
    public ObjectListResult listDir(String bucket, String dir, String start, int maxCount) throws IOException {
        //查询目录表
        start = Strings.nullToEmpty(start);
        Get get = new Get(Bytes.toBytes(dir));
        get.addFamily(HosUtil.DIR_SUBDIR_CF_BYTES);
        if (!Strings.isNullOrEmpty(start)) {
            get.setFilter(new QualifierFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(Bytes.toBytes(start))));
        }
        Result dirResult = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), get);
        List<HosObjectSummary> subDirs = null;
        if (!dirResult.isEmpty()) {
            subDirs = new ArrayList<>();
            for (Cell cell : dirResult.rawCells()) {
                HosObjectSummary summary = new HosObjectSummary();
                byte[] qualifierBytes = new byte[cell.getQualifierLength()];
                CellUtil.copyQualifierTo(cell, qualifierBytes, 0);
                String name = Bytes.toString(qualifierBytes);
                summary.setKey(dir + name + "/");
                summary.setName(name);
                summary.setLastModifyTime(cell.getTimestamp());
                summary.setMediaType("");
                summary.setBucket(bucket);
                summary.setLength(0);
                subDirs.add(summary);
                if (subDirs.size() > maxCount + 1) {
                    break;
                }
            }
        }
        //查询文件表
        String dirSeq = getDirSeqId(bucket, dir);
        byte[] objStart = Bytes.toBytes(dirSeq + "_" + start);
        Scan objScan = new Scan();
        objScan.setStartRow(objStart);
        objScan.setRowPrefixFilter(Bytes.toBytes(dirSeq + "_"));
        objScan.setMaxResultsPerColumnFamily(maxCount + 1);
        objScan.addFamily(HosUtil.OBJ_META_CF_BYTES);
        ResultScanner scanner = HBaseServiceImpl.getScanner(connection, HosUtil.getObjTableName(bucket), objScan);
        List<HosObjectSummary> objectSummaryList = new ArrayList<>();
        Result result = null;
        while (objectSummaryList.size() < maxCount + 2 && (result = scanner.next()) != null) {
            HosObjectSummary summary = resultToObjectSummary(result, bucket, dir);
        }
        if (scanner != null) {
            scanner.close();
        }
        if (subDirs != null && subDirs.size() > 0) {
            objectSummaryList.addAll(subDirs);
        }
        //返回给用户 maxCount
        Collections.sort(objectSummaryList);
        if (objectSummaryList.size() > maxCount) {
            objectSummaryList = objectSummaryList.subList(0, maxCount);
        }
        ObjectListResult listResult = new ObjectListResult();
        HosObjectSummary nextMarkerObj = objectSummaryList.size() > maxCount ? objectSummaryList.get(objectSummaryList.size()) : null;
        if (nextMarkerObj != null) {
            listResult.setNextMarker(nextMarkerObj.getKey());
        }
        listResult.setMaxKeyNumber(maxCount);
        if (objectSummaryList.size() > 0) {
            listResult.setMinKey(objectSummaryList.get(0).getKey());
            listResult.setMaxKey(objectSummaryList.get(objectSummaryList.size() - 1).getKey());
        }
        listResult.setObjectCount(objectSummaryList.size());
        listResult.setObjectSummaries(objectSummaryList);
        listResult.setBucket(bucket);
        return listResult;
    }

    @Override
    public ObjectListResult listByPrefix(String bucket, String dir, String start, String prefix, int maxCount) throws IOException {
        return null;
    }

    @Override
    public HosObject getObject(String bucket, String key) throws IOException {
        //判断是否为目录
        if (key.endsWith("/")) {
            //读取目录表
            Result result = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
            if (result.isEmpty()) {
                return null;
            }
            ObjectMetaData objectMetaData = new ObjectMetaData();
            objectMetaData.setBucket(bucket);
            objectMetaData.setKey(key);
            objectMetaData.setLength(0);
            objectMetaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
            HosObject object = new HosObject();
            object.setMetaData(objectMetaData);
            return object;
        }

        //读取文件表
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String seq = getDirSeqId(bucket, dir);
        if (seq == null) {
            return null;
        }
        String objKey = seq + "_" + key.substring(key.lastIndexOf("/") + 1);
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), objKey);
        if (result.isEmpty()) {
            return null;
        }
        HosObject object = new HosObject();
        long len = Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER));
        ObjectMetaData metaData = new ObjectMetaData();
        metaData.setBucket(bucket);
        metaData.setKey(key);
        metaData.setLastModifyTime(result.rawCells()[0].getTimestamp());
        metaData.setLength(len);
        metaData.setMediaType(Bytes.toString(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_MEDIATYPE_QUALIFIER)));
        byte[] b = result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_PROPS_QUALIFIER);
        if (b != null) {
            metaData.setAttrs(JsonUtil.fromJson(Map.class, Bytes.toString(b)));
        }
        object.setMetaData(metaData);
        //读取文件内容
        if (result.containsNonEmptyColumn(HosUtil.OBJ_CONT_CF_BYTES, HosUtil.OBJ_CONT_QUALIFIER)) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(result.getValue(HosUtil.OBJ_CONT_CF_BYTES, HosUtil.OBJ_CONT_QUALIFIER));
            object.setContent(inputStream);
        } else {
            String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seq;
            InputStream inputStream = this.fileStore.openFile(fileDir, key.substring(key.lastIndexOf("/") + 1));
            object.setContent(inputStream);
        }

        return object;
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception {
        //判断当前key是否为目录
        if (key.endsWith("/")) {
            //删除目录

            if (!isDirEmpty(bucket, key)) {
                throw new HosServerException(ErrorCodes.ERROR_PERMISSION_DENIED, "dir is not empty");
            }
            //获取锁
            InterProcessMutex lock = null;
            String lockey = key.replace("/", "_");
            lock = new InterProcessMutex(zkClient, "/hos/" + bucket + "/" + lockey);
            lock.acquire();
            //从父目录删除数据
            String dir1 = key.substring(0, key.lastIndexOf("/") + 1);
            String name = dir1.substring(key.lastIndexOf("/") + 1);
            if (name.length() > 0) {
                String parent = key.substring(0, key.lastIndexOf(name));
                HBaseServiceImpl.deleteColumnQualifier(connection, HosUtil.getDirTableName(bucket), parent, HosUtil.DIR_SUBDIR_CF, name);
            }
            HBaseServiceImpl.deleteRow(connection, HosUtil.getDirTableName(bucket), key);
            lock.release();
            return;

            //释放锁
        }

        //删除文件
        //首先从文件表获取文件的length
        String dir = key.substring(0, key.lastIndexOf("/") + 1);
        String name = key.substring(key.lastIndexOf("/") + 1);
        String seqId = getDirSeqId(bucket, dir);
        String objKey = seqId + "_" + name;
        Get get = new Get(objKey.getBytes());
        get.addColumn(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER);
        Result result = HBaseServiceImpl.getRow(connection, HosUtil.getObjTableName(bucket), get);
        if (result.isEmpty()) {
            return;
        }
        //通过length判断 hdfs or hbase
        long len = Bytes.toLong(result.getValue(HosUtil.OBJ_META_CF_BYTES, HosUtil.OBJ_LEN_QUALIFIER));
        if (len > HosUtil.FILE_STORE_THRESHOLD) {
            String fileDir = HosUtil.FILE_STORE_ROOT + "/" + bucket + "/" + seqId;
            fileStore.deleteFile(fileDir, name);
        }
        HBaseServiceImpl.deleteRow(connection, HosUtil.getObjTableName(bucket), objKey);

    }

    private boolean isDirEmpty(String bucket, String dir) throws IOException {
        return listDir(bucket, dir, null, 2).getObjectSummaries().size() == 0;
    }

    private boolean dirExist(String bucket, String key) throws IOException {
        return HBaseServiceImpl.existsRow(connection, HosUtil.getDirTableName(bucket), key);
    }

    private String getDirSeqId(String bucket, String key) throws IOException {
        Result res = HBaseServiceImpl.getRow(connection, HosUtil.getDirTableName(bucket), key);
        if (res.isEmpty()) {
            return null;
        }
        return Bytes.toString(res.getValue(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_QUALIFIER));
    }

    private String putDir(String bucket, String key) throws Exception {
        if (dirExist(bucket, key)) {
            return null;
        }

        //从zk获取锁
        InterProcessMutex lock = null;
        try {
            String lockey = key.replace("/", "_");
            lock = new InterProcessMutex(zkClient, "/hos/" + bucket + "/" + lockey);
            lock.acquire();
            String dir1 = key.substring(0, key.lastIndexOf("/"));
            String name = dir1.substring(dir1.lastIndexOf("/"));

            if (name.length() > 0) {
                String parent = dir1.substring(0, dir1.lastIndexOf("/") + 1);
                if (!dirExist(bucket, parent)) {
                    this.putDir(bucket, parent);
                }
                //在父目录添加sub 列族内 添加子项
                Put put = new Put(Bytes.toBytes(parent));
                put.addColumn(HosUtil.DIR_SUBDIR_CF_BYTES, Bytes.toBytes(name), Bytes.toBytes(1));
                HBaseServiceImpl.putRow(connection, HosUtil.getDirTableName(bucket), put);
            }
            //再去添加到目录表
            String seqId = getDirSeqId(bucket, key);
            String hash = seqId == null ? makeDirSeqId(bucket) : seqId;
            Put dirPut = new Put(key.getBytes());
            dirPut.addColumn(HosUtil.DIR_META_CF_BYTES, HosUtil.DIR_SEQID_QUALIFIER, Bytes.toBytes(hash));
            HBaseServiceImpl.putRow(connection, HosUtil.getDirTableName(bucket), dirPut);
            return hash;

        } finally {
            if (lock != null) {
                lock.release();
            }
        }
    }

    private String makeDirSeqId(String bucket) {

        long v = HBaseServiceImpl.incrementColumnValue(connection, HosUtil.BUCKET_DIR_SEQ_TABLE, bucket, HosUtil.BUCKET_DIR_SEQ_CF_BYTES, HosUtil.BUCKET_DIR_SEQ_QUALIFIER, 1);
        return String.format("%d%d", v % 64, v);
    }

}
