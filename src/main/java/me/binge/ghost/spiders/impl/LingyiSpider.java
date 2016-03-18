package me.binge.ghost.spiders.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import me.binge.ghost.spiders.AbstractSpider;
import me.binge.ghost.spiders.entity.Article;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.MDC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LingyiSpider extends AbstractSpider {

    private static final Logger logger = LoggerFactory.getLogger(LingyiSpider.class);

    private volatile boolean running = true;

    private static final Map<String, String> ARTICLE_URL_MAPPER = new HashMap<String, String>(){

        private static final long serialVersionUID = 1L;

        {
            put("http://www.lingyi.org/topics/lingyijingli", "http://www.zgyllt.cn/forum.php?mod=post&action=newthread&fid=2");
            put("http://www.lingyi.org/topics/lingyiqiuzhu", "http://www.zgyllt.cn/forum.php?mod=post&action=newthread&fid=38");
            put("http://www.lingyi.org/topics/minjianqitan", "http://www.zgyllt.cn/forum.php?mod=post&action=newthread&fid=41");
            put("http://www.lingyi.org/topics/guihualianpian", "http://www.zgyllt.cn/forum.php?mod=post&action=newthread&fid=49");
            put("http://www.lingyi.org/topics/qiwenyishi", "http://www.zgyllt.cn/forum.php?mod=post&action=newthread&fid=48");
            put("http://www.lingyi.org/topics/tansuofaxian", "http://www.zgyllt.cn/forum.php?mod=post&action=newthread&fid=50");
        }
    };

    private String subLastCrawlTime;

    @Override
    protected void doStart() {
        initWebDriver();
        webDriver.get("http://www.lingyi.org/");
        Set<String> articleIndexUrls = ARTICLE_URL_MAPPER.keySet();
        for (String url : articleIndexUrls) {
            crawlArticles(url);
            if (!running) {
                break;
            }
        }
        stop();
    }

    private void crawlArticles(String url) {
        MDC.put("url", url);
        webDriver.get(url);

        String html = webDriver.getPageSource();
        Document doc = Jsoup.parse(html);
        Elements articleEls = doc.select("#primary").select("article");
        if (articleEls == null || articleEls.isEmpty()) { // no articles
            logger.warn("no articles found");
            return;
        }
        for (Element articleEl : articleEls) {
            Elements dateEls = articleEl.select("span.date");
            if (dateEls == null || dateEls.isEmpty()) {
                logger.warn("date for article is not found in [[" + articleEl + "]]");
                continue;
            }
            String date = dateEls.get(0).text().trim();
            if (date.compareTo(this.subLastCrawlTime) < 0) { // 小于上次抓取时间
                logger.warn(date + " is before " + this.lastCrawlTime);
                return;
            }

            Elements urlEls = articleEl.select("h2.entry-title a");
            if (urlEls == null || urlEls.isEmpty()) {
                urlEls = articleEl.select("div.title-lz a");
            }
            if (urlEls == null || urlEls.isEmpty()) {
                logger.warn("url for article is not found in [[" + articleEl + "]]");
                continue;
            }
            Element urlEl = urlEls.get(0);
            String subject = urlEl.attr("title").trim();
            String articleUrl = urlEl.attr("href").trim();
            Article article = new Article();
            article.setSubject(subject);
            article.setUrl(articleUrl);
            article.setPostUrl(ARTICLE_URL_MAPPER.get(url));
            crawlArticle(article);
        }

        String nextUrl = null;
        Elements nextEls = doc.select("a.next");
        if (nextEls == null || nextEls.isEmpty()) {
            logger.warn("no next page element found");
        } else {
            nextUrl = nextEls.get(0).attr("href").trim();
        }
        MDC.remove("url");
        if (StringUtils.isNoneBlank(nextUrl)) {
            crawlArticles(nextUrl);
        }
    }

    private void crawlArticle(Article article) {
        String url = article.getUrl();
        MDC.put("articleUrl", url);
        try {
            webDriver.get(url);

            String html = webDriver.getPageSource();
            Document doc = Jsoup.parse(html);
            Elements dateEls = doc.select("span.date");
            if (dateEls == null || dateEls.isEmpty()) {
                logger.warn("no date element found.");
                return;
            }
            String date = dateEls.get(0).text().trim();
            if (date.compareTo(this.lastCrawlTime) < 0) {
                logger.warn(date + " is before " + this.lastCrawlTime);
                return;
            }

            Elements contentEls = doc.select("div.content-main");
            if (contentEls == null || contentEls.isEmpty()) {
                logger.warn("can not found content element");
                return;
            }
            Element contentEl = contentEls.get(0);
            Elements subContentEls = contentEl.children();
            if (subContentEls == null || subContentEls.isEmpty()) {
                logger.warn("can not found sub content elements");
                return;
            }
            StringBuilder content = new StringBuilder();
            for (Element subContentEl : subContentEls) {
                if ("div".equalsIgnoreCase(subContentEl.tagName())) {
                    continue;
                }
                if (content.length() > 0) {
                    content.append(Article.CONTENT_SEP);
                }
                content.append(subContentEl.text().trim());
            }

            article.setContent(content.toString());
            offerArticle(article);
        } catch (Exception e) {
            logger.error("crawl article errror, " + e.getMessage(), e);
        } finally {
            MDC.remove("articleUrl");
        }


    }

    @Override
    public String name() {
        return "lingyi";
    }

    @Override
    public void setLastCrawlTime(String lastCrawlTime) {
        super.setLastCrawlTime(lastCrawlTime);
        this.subLastCrawlTime = this.lastCrawlTime.substring(0, 10);
    }

    public static void main(String[] args) {

        LingyiSpider spider = new LingyiSpider();
        spider.setArticleQueue(new ConcurrentLinkedQueue<Article>());
//        spider.setLastCrawlTime(DateFormatUtils.format(DateUtils.addYears(new Date(), -1), "yyyy-MM-dd HH:mm:ss"));
        spider.setLastCrawlTime(null);
        spider.initWebDriver();

//        spider.crawlArticles("http://www.lingyi.org/topics/minjianqitan");
        spider.doStart();

    }
}
