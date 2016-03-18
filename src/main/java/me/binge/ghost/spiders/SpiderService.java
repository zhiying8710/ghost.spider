package me.binge.ghost.spiders;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.MDC;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;

public class SpiderService extends AbstractExecutionThreadService implements
        Service {

    private Spider spider;
    private AbstractScheduledService listener;

    public SpiderService(Spider spider) {
        if (spider == null) {
            throw new NullPointerException("spider is null");
        }
        this.spider = spider;
        this.addListener();
    }

    @Override
    protected void startUp() throws Exception {
        MDC.put("spiderName", spider.name());
        super.startUp();
    }

    @Override
    protected void shutDown() throws Exception {
        if (this.spider.isRunning()) {
            this.spider.stop();
        }
        this.listener.stopAsync();
        MDC.remove("spiderName");
    }

    @Override
    protected void run() throws Exception {
        this.spider.start();
    }

    public static Set<SpiderService> wrapSpiders(Set<Spider> spiders, CountDownLatch latch) {
        Set<SpiderService> spiderServices = new HashSet<>();
        for (Spider spider : spiders) {
            spider.setCrawlLatch(latch);
            spiderServices.add(new SpiderService(spider));
        }
        return spiderServices;
    }

    public void addListener() {
        this.listener = new AbstractScheduledService() {

            @Override
            protected Scheduler scheduler() {
                return Scheduler.newFixedRateSchedule(100, 1000,
                        TimeUnit.MILLISECONDS);
            }

            @Override
            protected void runOneIteration() throws Exception {
                if (SpiderService.this.state() == State.FAILED
                        || SpiderService.this.state() == State.TERMINATED) {
                    SpiderService.this.spider.stop();
                }
            }

        };
        this.listener.startAsync();
    }

}
