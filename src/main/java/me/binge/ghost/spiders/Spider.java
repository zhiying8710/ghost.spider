package me.binge.ghost.spiders;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import me.binge.ghost.spiders.entity.Article;

public interface Spider extends Comparable<Spider> {

    public String name();

    public void start();

    public void stop();

    public void setArticleQueue(Queue<Article> articles);

    public void setLastCrawlTime(String lastCrawlTime);

    public void setCrawlLatch(CountDownLatch latch);

    public boolean isRunning();

}
