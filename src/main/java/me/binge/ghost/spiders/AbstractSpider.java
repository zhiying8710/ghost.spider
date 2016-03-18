package me.binge.ghost.spiders;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import me.binge.ghost.spiders.entity.Article;
import me.binge.ghost.spiders.utils.Config;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

public abstract class AbstractSpider implements Spider {

    protected volatile boolean running = true;

    protected Queue<Article> articles;
    protected ChromeDriver webDriver;
    protected String lastCrawlTime;
    protected CountDownLatch latch;

    @Override
    public int compareTo(Spider o) {
        if (o == null) {
            return -1;
        }
        return o.name().compareTo(this.name());
    }

    @Override
    public abstract String name();

    @Override
    public void start() {
        if (this.articles == null) {
            throw new NullPointerException("the queue articles is null");
        }
        doStart();
    }

    protected abstract void doStart();

    @Override
    public void stop() {
        try {
            doStop();
        } finally {
            running = false;
            latch.countDown();
            try {
                webDriver.close();
                webDriver.quit();
            } catch (Exception e) {
                // do nothing.
            }
        }
    }

    protected void doStop() {}

    protected void initWebDriver() {
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_settings.images", 2); // 隐藏图片
        prefs.put("profile.default_content_setting_values.images", 2); // 隐藏图片
        options.setExperimentalOption("prefs", prefs);
        DesiredCapabilities caps = DesiredCapabilities.chrome();
        caps.setCapability(ChromeOptions.CAPABILITY, options);

        this.webDriver = new ChromeDriver(caps);
    }

    protected void sendKeys(By by, String value) {
        sendKeys(webDriver.findElement(by), value);
    }

    protected void sendKeys(WebElement el, String value) {
        if (StringUtils.isNotBlank(value)) {
            char[] vs = value.toCharArray();
            for (char v : vs) {
                el.sendKeys(Character.toString(v));
            }
        }
    }

    protected void sleep(TimeUnit timeUnit, long time) {
        try {
            TimeUnit.MILLISECONDS.convert(time, timeUnit);
        } catch (Exception e) {
            // do nothing
        }
    }

    @Override
    public void setArticleQueue(Queue<Article> articles) {
        this.articles = articles;
    }

    @Override
    public void setLastCrawlTime(String lastCrawlTime) {
        if (StringUtils.isBlank(lastCrawlTime)) {
            this.lastCrawlTime = DateFormatUtils.format(DateUtils.addYears(new Date(), 0 - Integer.valueOf(Config.get("max_crawl_dates", "30"))), "yyyy-MM-dd HH:mm:ss");
        } else {
            this.lastCrawlTime = lastCrawlTime;
        }
    }

    protected void offerArticle(Article article) {
        this.articles.offer(article);
    }

    protected Article pollArticle() {
        return this.articles.poll();
    }

    @Override
    public void setCrawlLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Spider)) {
            return false;
        }
        Spider o = (Spider) obj;
        return o.name().equals(this.name());
    }

    @Override
    public int hashCode() {
        return this.name().hashCode();
    }

}
