package com.runisys.web.rest;

import com.google.common.base.Splitter;
import com.runisys.CoreUtil;
import com.runisys.server.HosObject;
import com.runisys.server.IBucketService;
import com.runisys.server.IHosStore;
import com.runisys.server.ObjectListResult;
import com.runisys.usermgr.module.SystemRole;
import com.runisys.usermgr.module.UserInfo;
import com.runisys.web.security.ContextUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Hos服务文件管理接口
 */
@RestController
@RequestMapping("hos/v1/")
public class HosController extends BaseController {

    @Autowired
    @Qualifier("bucketServiceImpl")
    IBucketService bucketService;

    @Autowired
    @Qualifier("hosStore")
    IHosStore hosStore;

    private static long MAX_FILE_IN_MEMORY = 2 * 1024 * 1024;//2M
    private final int readBufferSize = 32 * 1024; //32K
    private static String TMP_DIR = System.getProperty("user.dir") + File.separator + "tmp";

    public HosController() {
        File file = new File(TMP_DIR);
        file.mkdirs();
    }

    //创建bucket
    @RequestMapping(value = "bucket", method = RequestMethod.POST)
    public Object createBucket(@RequestParam("bucket") String bucketName,
                               @RequestParam(name = "detail", required = false, defaultValue = "") String detail) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (!currentUser.getSystemRole().equals(SystemRole.VISITOR)) {
            bucketService.addBucket(currentUser, bucketName, detail);
            try {
                hosStore.createBucketStore(bucketName);
            } catch (IOException e) {
                bucketService.deleteBucket(bucketName);//删除刚插入的数据库数据
                return "error";
            }
            return "success";
        }
        return "permission denied";
    }


    //删除bucket
    @RequestMapping(value = "bucket", method = RequestMethod.DELETE)
    public Object deleteBucket(@RequestParam("bucket") String bucket) {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkBucketOwner(currentUser.getUserName(), bucket)) {
            try {
                hosStore.deleteBucketStore(bucket);
            } catch (IOException e) {
                return "delete error";
            }
            bucketService.deleteBucket(bucket);
            return "success";
        }
        return "permission denied";
    }

    /**
     * 获取bucket列表
     */
    @RequestMapping(value = "bucket/list", method = RequestMethod.GET)
    public Object getBucket() {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        return bucketService.getUserBuckets(currentUser.getUserId());
    }


    /**
     * 上传文件(创建目录)
     */
    @RequestMapping(value = "object", method = {RequestMethod.PUT, RequestMethod.POST})
    @ResponseBody
    public Object putObject(@RequestParam("bucket") String bucket,
                            @RequestParam("key") String key,
                            @RequestParam(value = "mediaType", required = false) String mediaType,
                            @RequestParam(value = "content", required = false) MultipartFile file,
                            HttpServletRequest request,
                            HttpServletResponse response) throws Exception {
        //创建目录 or 上传文件？
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            if (key.endsWith("/")) {
                hosStore.put(bucket, key, null, 0, mediaType, null);
            }
            ByteBuffer byteBuffer = null;
            if (file != null) {
                if (file.getSize() > MAX_FILE_IN_MEMORY) {
                    File dstFile = new File(TMP_DIR + File.separator + CoreUtil.getUUIDStr());
                    file.transferTo(dstFile);
                    file.getInputStream().close();
                    byteBuffer = new FileInputStream(dstFile).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.getSize());
                } else {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copy(file.getInputStream(), outputStream);
                    byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
                    file.getInputStream().close();
                }
                hosStore.put(bucket, key, byteBuffer, file.getSize(), mediaType, null);
                return "success";
            } else {
                return "error";
            }
        }

        //上传文件，如果文件过大，可能造成内存不足，缓存到本地
        return "permission denied";
    }


    //列出目录下文件(浏览)
    @RequestMapping(value = "object/list/dir", method = RequestMethod.GET)
    public ObjectListResult listObjectByDir(@RequestParam("bucket") String bucket,
                                            @RequestParam("dir") String dir,
                                            @RequestParam(value = "startKey", required = false, defaultValue = "") String start,
                                            HttpServletResponse response) throws IOException {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            if (!dir.startsWith("/") || !dir.endsWith("/")) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                response.getWriter().write("dir must start with / and end with /");
                return null;
            }
            if (start != null) {
                List<String> segs = StreamSupport.stream(Splitter.on("/").trimResults().omitEmptyStrings().split(start).spliterator(), false).collect(Collectors.toList());
                start = segs.get(segs.size() - 1);
            }
            hosStore.listDir(bucket, dir, start, 100);
        }
        return null;
    }

    //删除文件
    @RequestMapping(value = "object", method = RequestMethod.DELETE)
    public Object deleteObject(@RequestParam("bucket") String bucket,
                               @RequestParam("key") String key) throws Exception {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            hosStore.deleteObject(bucket, key);
            return "success";
        }
        return "permission denied";
    }

    //下载文件
    @RequestMapping(value = "object/content", method = RequestMethod.GET)
    public void getObject(@RequestParam("bucket") String bucket,
                          @RequestParam("key") String key,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        UserInfo currentUser = ContextUtil.getCurrentUser();
        if (operationAccessControl.checkPermission(currentUser.getUserId(), bucket)) {
            HosObject object = hosStore.getObject(bucket, key);
            if (object == null) {
                response.setStatus(400);
                return;
            }
            //todo 设置headers

            ServletOutputStream outputStream = response.getOutputStream();
            InputStream inputStream = object.getContent();
            try {
                byte[] buffer = new byte[readBufferSize];
                int len = -1;
                while ((len = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, len);
                }
                response.flushBuffer();
            } finally {
                inputStream.close();
                outputStream.close();
            }

        }
    }
}
