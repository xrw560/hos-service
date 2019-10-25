package com.runisys.server;

import com.runisys.ErrorCodes;
import com.runisys.HosConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Hdfs;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class HdfsServiceImpl implements IHdfsService {
    private static Logger logger = Logger.getLogger(HdfsServiceImpl.class);

    private FileSystem fileSystem;
    private long defaultBlockSize = 128 * 1024 * 1024;
    private long initBlockISze = defaultBlockSize / 2;

    public HdfsServiceImpl() throws URISyntaxException, IOException {
        //1. 读取HDFS相关的配置信息
        HosConfiguration hosConfiguration = HosConfiguration.getConfiguration();
        String confDir = hosConfiguration.getString("hadoop.conf.dir");
        String hdfsUri = hosConfiguration.getString("hadoop.uri");
        //hdfs://localhost:9000
        //2. 通过配置获取一个filesystem的实例
        Configuration configuration = new Configuration();
        configuration.addResource(new Path(confDir + "/hdfs-site.xml"));
        configuration.addResource(new Path(confDir + "/core-site.xml"));
        fileSystem = FileSystem.get(new URI(hdfsUri), configuration);
    }

    @Override
    public void saveFile(String dir, String name, InputStream inputStream, long length, short replication) throws IOException {
        //1. 判断dir是否存在，不存在则创建
        Path dirPath = new Path(dir);
        try {
            if (!fileSystem.exists(dirPath)) {
                boolean succ = fileSystem.mkdirs(dirPath, FsPermission.getDirDefault());
                logger.info("create dir " + dirPath);
                if (!succ) {
                    throw new HosServerException(ErrorCodes.ERROR_HDFS, "create dir " + dirPath + " error");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //2. 保存文件
        Path path = new Path(dir + "/" + name);
        long blockSize = length <= initBlockISze ? initBlockISze : defaultBlockSize;
        FSDataOutputStream outputStream = fileSystem.create(path, true, 512 * 1024, replication, blockSize);
        try {
            fileSystem.setPermission(path, FsPermission.getFileDefault());
            byte[] buffer = new byte[1024 * 1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }

    }

    @Override
    public void deleteFile(String dir, String name) throws IOException {
        fileSystem.delete(new Path(dir + "/" + name), false);
    }

    @Override
    public InputStream openFile(String dir, String name) throws IOException {

        return fileSystem.open(new Path(dir + "/" + name));
    }

    @Override
    public void mkDir(String dir) throws IOException {
        fileSystem.mkdirs(new Path(dir));
    }

    @Override
    public void deleteDir(String dir) throws IOException {
        fileSystem.delete(new Path(dir), true);
    }
}
