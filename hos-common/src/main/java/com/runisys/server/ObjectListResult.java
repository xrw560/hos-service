package com.runisys.server;

import lombok.Data;

import java.util.List;

@Data
public class ObjectListResult {

    private String bucket;
    private String maxKey;
    private String minKey;
    private String nextMarker;
    private int maxKeyNumber;
    private int objectCount;
    private String listId;
    private List<HosObjectSummary> objectSummaries;
}
