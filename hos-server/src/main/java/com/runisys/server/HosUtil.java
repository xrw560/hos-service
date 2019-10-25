package com.runisys.server;

public class HosUtil {

    static final String DIR_TABLE_PREFIX = "hos_dir_";
    static final String OBJ_TABLE_PREFIX = "hos_obj_";

    static final String DIR_META_CF = "cf";
    static final byte[] DIR_META_CF_BYTES = DIR_META_CF.getBytes();
    static final String DIR_SUBDIR_CF = "sub";
    static final byte[] DIR_SUBDIR_CF_BYTES = DIR_SUBDIR_CF.getBytes();

    static final String OBJ_META_CF = "cf";
    static final byte[] OBJ_META_CF_BYTES = OBJ_META_CF.getBytes();
    static final String OBJ_CONT_CF = "c";
    static final byte[] OBJ_CONT_CF_BYTES = OBJ_CONT_CF.getBytes();


    static final byte[] DIR_SEQID_QUALIFIER = "u".getBytes();
    static final byte[] OBJ_CONT_QUALIFIER = "c".getBytes();
    static final byte[] OBJ_LEN_QUALIFIER = "l".getBytes();
    static final byte[] OBJ_PROPS_QUALIFIER = "p".getBytes();
    static final byte[] OBJ_MEDIATYPE_QUALIFIER = "m".getBytes();

    static final String FILE_STORE_ROOT = "/hos";
    static final int FILE_STORE_THRESHOLD = 20 * 1024 * 1024;

    static final String BUCKET_DIR_SEQ_TABLE = "hos_dir_seq";
    static final String BUCKET_DIR_SEQ_CF = "s";
    static final byte[] BUCKET_DIR_SEQ_CF_BYTES = BUCKET_DIR_SEQ_CF.getBytes();
    static final byte[] BUCKET_DIR_SEQ_QUALIFIER = "s".getBytes();

    static String getDirTableName(String bucketName) {
        return DIR_TABLE_PREFIX + bucketName;
    }

    static String getObjTableName(String bucketName) {
        return OBJ_TABLE_PREFIX + bucketName;
    }

    static String[] getDirColumnFamily() {
        return new String[]{DIR_META_CF, DIR_SUBDIR_CF};
    }

    static String[] getObjColumnFamily() {
        return new String[]{OBJ_META_CF, OBJ_META_CF};
    }
}
