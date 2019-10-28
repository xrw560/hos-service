package com.runisys.sdk;

import com.runisys.server.BucketModel;
import com.runisys.server.HosObjectSummary;
import com.runisys.server.PutRequest;
import com.runisys.server.util.JsonUtil;
import okhttp3.*;
//import org.apache.log4j.Logger;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HosClientImpl implements HosClient {

    static Logger logger = Logger.getLogger(HosClientImpl.class.getName());
    private String hosServer;
    private String schema;
    private String host;
    private int port = 80;
    private String token;
    private OkHttpClient client;

    public HosClientImpl(String endpoints, String token) {
        //http://127.0.0.1:9080 http://127.0.0.1:9080
        this.hosServer = endpoints;
        String[] ss = endpoints.split("://", 2);
        this.schema = ss[0];
        String[] ss1 = ss[1].split(":", 2);
        this.host = ss1[0];
        this.port = Integer.parseInt(ss1[1]);
        this.token = token;

        ConnectionPool pool = new ConnectionPool(10, 30, TimeUnit.SECONDS);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(120L, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .connectionPool(pool);
        Interceptor interceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = null;
                boolean success = false;
                int tryCount = 0;
                int maxLimit = 5;
                while (!success && tryCount < maxLimit) {
                    if (tryCount > 0) {
                        logger.info("intercept:" + " retry request - " + tryCount);
                    }
                    response = chain.proceed(request);
                    if (response.code() == 404) {
                        break;
                    }
                    success = response.isSuccessful();
                    tryCount++;
                    if (success) {
                        return response;
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return response;
            }
        };
        client = httpClientBuilder.addInterceptor(interceptor).build();
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
    }

    private Headers buildHeaders(Map<String, String> attrs, String token, String contentEncoding) {
        Map<String, String> headerMap = new HashMap<>();
        if (contentEncoding != null) {
            headerMap.put("content-encoding", contentEncoding);
        }
        headerMap.put("X-Auth-Token", token);
        if (attrs != null && attrs.size() > 0) {
            attrs.forEach(new BiConsumer<String, String>() {
                @Override
                public void accept(String s, String s2) {
                    headerMap.put("x-hos-attr_" + s, s2);
                }
            });
        }
        Headers headers = Headers.of(headerMap);
        return headers;
    }

    @Override
    public void createBucket(String bucketName, String detail) throws IOException {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/bucket")
                        .addQueryParameter("bucket", bucketName)
                        .addQueryParameter("detail", detail)
                        .build())
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String msg = request.body().toString();
            response.close();
            throw new IOException("create bucket error " + msg);
        }
        response.close();
    }

    @Override
    public void deleteBucket(String bucketName) throws IOException {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/bucket")
                        .addQueryParameter("bucket", bucketName)
                        .build())
                .delete(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String msg = request.body().toString();
            response.close();
            throw new IOException("delete bucket error " + msg);
        }
        response.close();
    }

    @Override
    public List<BucketModel> listBucket() throws IOException {
        Headers headers = this.buildHeaders(null, token, null);
        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/bucket/list")
                        .build())
                .get()
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String msg = request.body().toString();
            response.close();
            throw new IOException("list bucket error " + msg);
        }

        String json = response.body().toString();
        List<BucketModel> bucketModels = JsonUtil.fromJsonList(BucketModel.class, json);

        response.close();
        return bucketModels;
    }

    @Override
    public void putObject(PutRequest putRequest) throws IOException {
        //判断是否为上传文件 （创建目录）
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = null;

        if(putRequest.getContent()!=null){
            if(putRequest.getMediaType()==null){
                putRequest.setMediaType("application/octet-stream");
            }
            requestBody = RequestBody.create(MediaType.parse(putRequest.getMediaType()),putRequest.getContent());
        }
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        builder.addFormDataPart("bucket",putRequest.getBucket());
        if(putRequest.getMediaType()!=null){
            builder.addFormDataPart("mediaType",putRequest.getMediaType());
        }
        builder.addFormDataPart("key",putRequest.getKey());
        if(requestBody!=null){
            builder.addFormDataPart("content","content",requestBody);
        }
        requestBody = builder.build();


        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/object")
                        .build())
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String msg = request.body().toString();
            response.close();
            throw new IOException("delete object error " + msg);
        }
        response.close();
    }

    @Override
    public void putObject(String bucket, String key) throws IOException {
        PutRequest putRequest = new PutRequest(bucket,key,null);
        putObject(putRequest);
    }

    @Override
    public void putObject(String bucket, String key, byte[] content, String mediaType) throws IOException {
        PutRequest putRequest = new PutRequest(bucket,key,content,mediaType);
        putObject(putRequest);
    }

    @Override
    public void deleteObject(String bucket, String key) throws IOException {
        Headers headers = this.buildHeaders(null, token, null);
        RequestBody requestBody = RequestBody.create(null, new byte[0]);
        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/object")
                        .addQueryParameter("bucket", bucket)
                        .addQueryParameter("key", key)
                        .build())
                .delete(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String msg = request.body().toString();
            response.close();
            throw new IOException("delete object error " + msg);
        }
        response.close();
    }

    @Override
    public HosObjectSummary getObjectSummary(String bucket, String key) throws IOException {
        Headers headers = this.buildHeaders(null, token, null);
        Request request = new Request.Builder()
                .headers(headers)
                .url(new HttpUrl.Builder()
                        .scheme(schema)
                        .host(host)
                        .port(port)
                        .addPathSegment("/hos/v1/object/info")
                        .addQueryParameter("bucket", bucket)
                        .addQueryParameter("key", key)
                        .build())
                .get()
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            String msg = request.body().toString();
            response.close();
            throw new IOException("get object summary error" + msg);
        }

        String json = response.body().toString();
        HosObjectSummary objectSummary = JsonUtil.fromJson(HosObjectSummary.class, json);

        response.close();
        return objectSummary;
    }
}
