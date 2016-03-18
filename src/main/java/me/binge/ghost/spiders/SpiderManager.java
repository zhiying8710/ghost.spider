package me.binge.ghost.spiders;

import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import me.binge.ghost.spiders.entity.Article;
import me.binge.ghost.spiders.exception.NotImplementationException;
import me.binge.ghost.spiders.impl.publish.PublishSpider;
import me.binge.ghost.spiders.utils.ZookeeperUtils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ServiceManager;

public class SpiderManager {

    private static final Logger logger = LoggerFactory.getLogger(SpiderManager.class);

    private static ServiceManager spiderServiceManager;

    @SuppressWarnings("unchecked")
    private static Class<? extends Spider> check(String spiderClz) {
        try {
            Class<?> spiderClass = Class.forName(spiderClz);
            boolean isSpiderClass = false;

            Class<?> superclass = spiderClass.getSuperclass();
            if (superclass == AbstractSpider.class) {
                isSpiderClass = true;
            }

            if (!isSpiderClass) {
                Class<?>[] interfaces = spiderClass.getInterfaces();
                for (Class<?> infce : interfaces) {
                    if (infce == Spider.class) {
                        isSpiderClass = true;
                        break;
                    }
                }
            }

            if (!isSpiderClass) {
                throw new NotImplementationException(spiderClz + " is not a "
                        + Spider.class.getName() + "'s implementation.");
            }
            return (Class<? extends Spider>) spiderClass;
        } catch (Exception e) {
            logger.error(
                    "load spider class " + spiderClz + " error, "
                            + e.getMessage(), e);
            return null;
        }
    }

    private SpiderManager() {
    }

    public static void boot() {
        CountDownLatch latch = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            Class.forName("me.binge.ghost.spiders.utils.Config");
            boolean lock = ZookeeperUtils.lockThis();
            if (!lock) {
                logger.error("the spider is currently running on other instance.");
                return;
            }

            String lastCrawlTime = ZookeeperUtils.getLastCrawlTime();

            final Queue<Article> articles = new ConcurrentLinkedQueue<>();
            Path path = Paths.get(ClassLoader.getSystemResource("spiders").toURI());
            Files.copy(path, baos);
            String sSpiderClzs = new String(baos.toByteArray(), Charset.forName("utf-8"));
            Set<Spider> readySpiders = new HashSet<>();
            if (StringUtils.isNoneBlank(sSpiderClzs)) {
                String[] spiderClzs = sSpiderClzs.split("\n");
                for (String spiderClz : spiderClzs) {
                    spiderClz = spiderClz.trim();
                    Class<? extends Spider> spiderClass = check(spiderClz);
                    if (spiderClass == null) {
                        continue;
                    }
                    Spider spider = null;
                    try {
                        Constructor<? extends Spider> spiderConstructor = spiderClass
                                .getConstructor();
                        spider = spiderConstructor.newInstance();
                    } catch (Exception e) {
                        logger.error("init spider " + spiderClass.getName()
                                + " error, " + e.getMessage(), e);
                        continue;
                    }
                    if (readySpiders.contains(spider)) {
                        continue;
                    }
                    spider.setArticleQueue(articles);
                    spider.setLastCrawlTime(lastCrawlTime);
                    readySpiders.add(spider);
                }
            }

            if (!readySpiders.isEmpty()) {
                latch = new CountDownLatch(readySpiders.size() + 1);
                final PublishSpider publishSpider = new PublishSpider();
                publishSpider.setArticleQueue(articles);
                readySpiders.add(publishSpider);
                spiderServiceManager = new ServiceManager(SpiderService.wrapSpiders(readySpiders, latch));
                ZookeeperUtils.setLastCrawlTime();
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        spiderServiceManager.stopAsync();
                        spiderServiceManager = null;
                        articles.clear();
                    }
                }));
            } else {
                logger.warn("no spider to run");
            }

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        if (spiderServiceManager != null) {
            spiderServiceManager.startAsync();
            logger.info("spiders are running");
            while (latch != null && latch.getCount() != 0) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (Exception e) {
                }
            }
            try {
                Process process = new ProcessBuilder("taskkill.exe /F /IM chromedriver.exe".split(" ")).start();
                process.waitFor();
                process.destroy();
            } catch (Exception e) {
            }
            logger.info("spiders are stoped, quit.");
        }

    }

}
