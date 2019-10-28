package com.runisys.sdk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HosClientFactory {
    private static Map<String, HosClient> clientCache = new ConcurrentHashMap<>();

    public static HosClient getOrCreateHosClient(String endpoints, String token){
        String key = endpoints+"_"+token;
        //判断clientcache含有key的hosclient

        if(clientCache.containsKey(key)){
            return clientCache.get(key);
        }
        //创建client 并放到cache
        HosClient client = new HosClientImpl(endpoints,token);
        clientCache.put(key,client);
        return client;
    }
}
