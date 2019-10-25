package com.runisys.server;

import java.io.IOException;
import java.io.InputStream;

public interface IHdfsService {
    public void saveFile(String dir, String name, InputStream inputStream, long length, short replication) throws IOException;

    public void deleteFile(String dir, String name) throws IOException;

    public InputStream openFile(String dir, String name) throws IOException;

    public void mkDir(String dir) throws IOException;

    public void deleteDir(String dir) throws IOException;

}
