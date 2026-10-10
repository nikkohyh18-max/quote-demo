package com.demo.quote.service;

import com.demo.quote.model.Quote;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 报价生成与分发。
 *
 * <p>后端不保存历史：只持有「最新一条」，新报价生成后立刻推给订阅者，推完就不再持有。
 * 历史流水由页面自己累积，因此刷新页面会清零，断线期间的报价也无法补发
 * （客户端仍能通过序列号跳变发现缺口）。
 */
@Service
public class QuoteService {

    private final AtomicInteger serialNo = new AtomicInteger(0);
    private final Random random = new Random();

    /** 两条报价之间的基础间隔，可用 quote.interval-ms 调整。 */
    private final long intervalMs;
    /** 在基础间隔上叠加的随机抖动上限，可用 quote.interval-jitter-ms 调整。 */
    private final int jitterMs;

    private static final String[] QUOTE_PERSONS = {"张三", "李四", "王五", "赵六"};
    private static final String[] QUOTE_ORGS = {"机构A", "机构B", "机构C"};
    private static final String[] DIRECTIONS = {"买入", "卖出"};
    private static final String[] SETTLE_SPEEDS = {"T+0", "T+1", "T+2"};

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 精确到毫秒，否则同一秒内的多条报价在页面上时间完全一样、看不出先后。 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 最新一条报价，整体替换。
     * 每个字段各自 volatile 的话，读取时可能拼出“价格来自第 N 条、报价人来自第 N+1 条”的结果。
     */
    private volatile Quote latest;

    /**
     * SSE 订阅者。只有一个生成线程调用，推送顺序天然与生成顺序一致，不需要额外加锁。
     */
    private final List<Consumer<Quote>> listeners = new CopyOnWriteArrayList<Consumer<Quote>>();

    public QuoteService(@Value("${quote.interval-ms:3000}") long intervalMs,
                        @Value("${quote.interval-jitter-ms:120}") int jitterMs) {
        this.intervalMs = Math.max(1L, intervalMs);
        this.jitterMs = Math.max(0, jitterMs);
    }

    @PostConstruct
    void start() {
        // 启动后台线程，随机间隔生成报价
        Thread generator = new Thread(this::generateLoop, "quote-generator");
        generator.setDaemon(true);
        generator.start();
    }

    private void generateLoop() {
        try {
            updateQuote();
            while (true) {
                Thread.sleep(intervalMs + (jitterMs > 0 ? random.nextInt(jitterMs) : 0));
                updateQuote();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateQuote() {
        LocalDateTime now = LocalDateTime.now();
        Quote quote = new Quote(
            serialNo.incrementAndGet(),
            BigDecimal.valueOf(100 + random.nextDouble() * 100).setScale(4, BigDecimal.ROUND_HALF_UP),
            now.format(TIME_FMT),
            QUOTE_PERSONS[random.nextInt(QUOTE_PERSONS.length)],
            QUOTE_ORGS[random.nextInt(QUOTE_ORGS.length)],
            now.format(DATE_FMT),
            BigDecimal.valueOf(1000 + random.nextInt(9000)).setScale(2, BigDecimal.ROUND_HALF_UP),
            DIRECTIONS[random.nextInt(DIRECTIONS.length)],
            SETTLE_SPEEDS[random.nextInt(SETTLE_SPEEDS.length)]
        );
        latest = quote;
        for (Consumer<Quote> listener : listeners) {
            try {
                listener.accept(quote);
            } catch (RuntimeException e) {
                // 连接已断：摘掉它，别让一个坏订阅者影响后续报价
                listeners.remove(listener);
            }
        }
    }

    /** 当前最新一条报价；服务刚启动、第一条还没生成时为 null。 */
    public Quote getLatestQuote() {
        return latest;
    }

    /**
     * 订阅后续报价。没有历史可补，订阅者从下一条开始收。
     */
    public void subscribe(Consumer<Quote> listener) {
        listeners.add(listener);
    }

    public void unsubscribe(Consumer<Quote> listener) {
        listeners.remove(listener);
    }

    /** 当前订阅者数量，用于观察连接是否正常回收。 */
    public int getSubscriberCount() {
        return listeners.size();
    }
}
