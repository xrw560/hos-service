package com.runisys.server;

import com.runisys.ErrorCodes;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FilterList;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class HBaseServiceImpl {

    //1. 创建表
    public static boolean createTable(Connection connection, String tableName, String[] cfs, byte[][] splitKeys) {
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            if (admin.tableExists(tableName)) {
                return false;
            }
            HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
            Arrays.stream(cfs).forEach(cf -> {
                HColumnDescriptor hColumnDescriptor = new HColumnDescriptor(cf);
                hColumnDescriptor.setMaxVersions(1);
                tableDescriptor.addFamily(hColumnDescriptor);
            });
            admin.createTable(tableDescriptor, splitKeys);
        } catch (Exception e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "create table error");
        }
        return true;
    }

    //2. 删除表
    public static boolean deleteTable(Connection connection, String tableName) {
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            admin.disableTable(tableName);
            admin.deleteTable(tableName);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete table error");
        }
        return true;
    }

    //3. 删除列族
    public static boolean deleteColumnFamily(Connection connection, String tableName, String cf) {
        try (HBaseAdmin admin = (HBaseAdmin) connection.getAdmin()) {
            admin.deleteColumn(tableName, cf);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete cf error");
        }
        return true;
    }

    //4. 删除列
    public static boolean deleteColumnQualifier(Connection connection, String tableName, String rowkey, String cf, String column) {
        Delete delete = new Delete(rowkey.getBytes());
        delete.addColumn(cf.getBytes(), column.getBytes());
        return deleteRow(connection, tableName, delete);
    }

    public static boolean deleteRow(Connection connection, String tableName, Delete delete) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.delete(delete);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "delete  error");
        }
        return true;
    }

    //5. 删除行
    public static boolean deleteRow(Connection connection, String tableName, String rowkey) {
        Delete delete = new Delete(rowkey.getBytes());
        return deleteRow(connection, tableName, delete);

    }

    //6. 读取行
    public static Result getRow(Connection connection, String tableName, Get get) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.get(get);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get data error");
        }
    }

    public static Result getRow(Connection connection, String tableName, String rowkey) {
        Get get = new Get(rowkey.getBytes());
        return getRow(connection, tableName, get);
    }

    //7. 获取Scanner
    public static ResultScanner getScanner(Connection connection, String tableName, Scan scan) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.getScanner(scan);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get scanner error");
        }
    }

    public static ResultScanner getScanner(Connection connection, String tableName, String startkey, String endKey, FilterList filterList) {
        Scan scan = new Scan();
        scan.setStartRow(startkey.getBytes());
        scan.setStopRow(endKey.getBytes());
        scan.setFilter(filterList);
        scan.setCaching(1000);
        return getScanner(connection, tableName, scan);
    }


    //8. 插入行
    public static boolean putRow(Connection connection, String tableName, Put put) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(put);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get put error");
        }
        return true;
    }

    //9. 批量插入
    public static boolean putRow(Connection connection, String tableName, List<Put> puts) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            table.put(puts);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get row error");
        }
        return true;
    }

    //10 incrementColumnValue 通过这个方法，生成目录的seqid
    public static long incrementColumnValue(Connection connection, String tableName, String row, String cf, String qualifier, int num) {
        try (Table table = connection.getTable(TableName.valueOf(tableName))) {
            return table.incrementColumnValue(row.getBytes(), cf.getBytes(), qualifier.getBytes(), num);
        } catch (IOException e) {
            e.printStackTrace();
            throw new HosServerException(ErrorCodes.ERROR_HBASE, "get row error");
        }
    }

}
