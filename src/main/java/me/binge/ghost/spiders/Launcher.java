package me.binge.ghost.spiders;

import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.log4j.Logger;

public class Launcher {

    private static final Logger logger = Logger.getLogger(Launcher.class);

    private Launcher() {
    }

    static {
        Path webDriverPath = null;
        try {
            webDriverPath = Paths.get(ClassLoader.getSystemResource("chromedriver.exe").toURI());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        logger.info("find chromedriver.exe : " + webDriverPath);
        System.getProperties().setProperty("webdriver.chrome.driver", webDriverPath.toString());
    }


    public static void main(String[] args) throws Exception {

        logger.info("sys working..., PID: " + ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        SpiderManager.boot();

    }

}
