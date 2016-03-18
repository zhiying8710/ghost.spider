package me.binge.ghost.spiders.impl.publish;

import java.util.concurrent.TimeUnit;

import me.binge.ghost.spiders.AbstractSpider;
import me.binge.ghost.spiders.entity.Article;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishSpider extends AbstractSpider {

    private static final Logger logger = LoggerFactory.getLogger(PublishSpider.class);

    @Override
    protected void doStart() {
        initWebDriver();

        webDriver.get("http://www.zgyllt.cn/forum.php");
        sendKeys(webDriver.findElementById("ls_username"), "鬼娃子");
        sendKeys(webDriver.findElementById("ls_password"), "1234567890");

        webDriver.findElementById("lsform").submit();

        while (!webDriver.getPageSource().contains("退出")) {
            sleep(TimeUnit.MILLISECONDS, 500);
            continue;
        }

        monitorArticles();
    }

    private void monitorArticles() {

        while (running && latch.getCount() > 1) {

            Article article = this.pollArticle();
            if (article == null) {
                sleep(TimeUnit.SECONDS, 1);
                continue;
            }
            String postUrl = article.getPostUrl();
            if (StringUtils.isBlank(postUrl)) {
                logger.error("no post url in " + article + ", drop it.");
                continue;
            }
            MDC.put("url", postUrl);
            try {
                if (StringUtils.isBlank(postUrl)) {
                    logger.warn("can not get post page from articel [["
                            + article + "]]");
                    continue;
                }
                webDriver.get(postUrl);
                sendKeys(webDriver.findElementById("subject"),
                        article.getSubject());
                WebElement iframe = null;
                while (iframe == null
                        || !iframe.getTagName().toLowerCase().equals("iframe")) {
                    sleep(TimeUnit.MILLISECONDS, 500);
                    iframe = webDriver.findElementById("e_iframe");
                }

                webDriver.switchTo().frame(iframe);

                WebElement contentEl = webDriver
                        .findElementByCssSelector("body");
                String content = article.getContent();
                String[] contents = content.split(Article.CONTENT_SEP);
                for (String c : contents) {
                    sendKeys(contentEl, c);
                    contentEl.sendKeys(Keys.ENTER);
                }

                webDriver.switchTo().defaultContent();
                webDriver.findElementById("postsubmit").click();
                while (webDriver.getCurrentUrl().equals(postUrl)) {
                    sleep(TimeUnit.MILLISECONDS, 500);
                }
            } catch (Exception e) { // 防御容错
                logger.error("publish article error, " + e.getMessage(), e);
            }
            MDC.remove("url");
        }
        if (running) {
            stop();
        }

    }

    @Override
    public String name() {
        return "publish";
    }

}
