package me.binge.ghost.spiders.utils;

import java.util.Date;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.commons.lang3.time.DateFormatUtils;

public class ZookeeperUtils {

    private static final String SERVER = "sonovel.com:2181";
    private static final String ROOT = "/ghost/spider/";
    private static final String LOCK = "lock";
    private static final String LAST_CRAWL_TIME = "last_crawl_time";
    private static ZkClient zkClient;

    static {
        zkClient = new ZkClient(SERVER, 30 * 60, 60 * 60);
        zkClient.createPersistent(ROOT.substring(0, ROOT.length() - 1), true);
        zkClient.createPersistent(ROOT + LAST_CRAWL_TIME, true);
    }

    public static String getLastCrawlTime() {
        return zkClient.readData(ROOT + LAST_CRAWL_TIME);
    }

    public static void setLastCrawlTime() {
        zkClient.writeData(ROOT + LAST_CRAWL_TIME, DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
    }

    public static boolean lockThis() {
        try {
            zkClient.createEphemeral(ROOT + LOCK);
        } catch (ZkNodeExistsException zknee) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void release() {
        if (zkClient != null) {
            try {
                zkClient.close();
            } catch (Exception e) {
                // do nothing
            }
            zkClient = null;
        }
    }

}
