package com.runisys.sdk;

import com.runisys.server.BucketModel;

import java.io.IOException;
import java.util.List;

public class SdkTest {

    private static  String token = "";
    private static String endPoints = "http://127.0.0.1:9080";

    public static void main(String[] args) throws IOException {
        HosClient client = HosClientFactory.getOrCreateHosClient(endPoints, token);
        List<BucketModel> bucketModels = client.listBucket();
        bucketModels.forEach(bucketModel -> {
            System.out.printf(bucketModel.getBucketName()+"|"+bucketModel.getCreator());
        });
    }

}
