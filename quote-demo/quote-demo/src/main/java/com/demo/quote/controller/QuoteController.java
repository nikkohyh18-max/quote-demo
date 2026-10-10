package com.demo.quote.controller;

import com.demo.quote.model.Quote;
import com.demo.quote.service.QuoteService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@RestController
public class QuoteController {

    private final QuoteService quoteService;

    public QuoteController(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping("/api/quote")
    public Quote getQuote() {
        return quoteService.getLatestQuote();
    }

    /**
     * 轮询降级路径：后端不存历史，所以最多只能返回「最新一条」。
     * since 小于最新序列号时返回这一条，否则返回空数组。
     *
     * <p>注意：报价生成间隔小于客户端轮询周期时，这条路径会漏报价 —— 后端没有历史可补。
     * 正常路径是 {@link #streamQuotes()} 的 SSE 推送。
     */
    @GetMapping("/api/quotes")
    public List<Quote> getQuotes(@RequestParam(name = "since", defaultValue = "0") int since) {
        Quote latest = quoteService.getLatestQuote();
        if (latest == null || latest.getSerialNo() <= since) {
            return Collections.emptyList();
        }
        return Collections.singletonList(latest);
    }

    /**
     * SSE 推送：报价一生成就发出去。
     * 后端不保存历史，订阅者从下一条报价开始收，连接前的报价不补发。
     */
    @GetMapping(value = "/api/quotes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuotes() {
        // 0 表示不超时；报价流是长连接，Tomcat 默认 30 秒超时会一直断
        SseEmitter emitter = new SseEmitter(0L);

        Consumer<Quote> listener = quote -> send(emitter, quote);
        emitter.onCompletion(() -> quoteService.unsubscribe(listener));
        emitter.onTimeout(() -> quoteService.unsubscribe(listener));
        emitter.onError(e -> quoteService.unsubscribe(listener));

        quoteService.subscribe(listener);
        return emitter;
    }

    private void send(SseEmitter emitter, Quote quote) {
        try {
            emitter.send(SseEmitter.event()
                .id(String.valueOf(quote.getSerialNo()))
                .name("quote")
                .data(quote, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            // 客户端已断开：结束这个 emitter，并抛出让 QuoteService 把监听摘掉
            emitter.completeWithError(e);
            throw new IllegalStateException("SSE client gone", e);
        }
    }
}
