package com.demo.quote.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Quote {
    private int serialNo;
    private BigDecimal price;
    private String time;
    private String quotePerson;
    private String quoteOrg;
    private String quoteDate;
    private BigDecimal quoteVolume;
    private String quoteDirection;
    private String settleSpeed;

    public Quote(int serialNo, BigDecimal price, LocalDateTime time, String quotePerson, String quoteOrg,
                 String quoteDate, BigDecimal quoteVolume, String quoteDirection, String settleSpeed) {
        this.serialNo = serialNo;
        this.price = price;
        this.time = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.quotePerson = quotePerson;
        this.quoteOrg = quoteOrg;
        this.quoteDate = quoteDate;
        this.quoteVolume = quoteVolume;
        this.quoteDirection = quoteDirection;
        this.settleSpeed = settleSpeed;
    }

    public Quote(int serialNo, BigDecimal price, String time, String quotePerson, String quoteOrg,
                 String quoteDate, BigDecimal quoteVolume, String quoteDirection, String settleSpeed) {
        this.serialNo = serialNo;
        this.price = price;
        this.time = time;
        this.quotePerson = quotePerson;
        this.quoteOrg = quoteOrg;
        this.quoteDate = quoteDate;
        this.quoteVolume = quoteVolume;
        this.quoteDirection = quoteDirection;
        this.settleSpeed = settleSpeed;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getQuotePerson() {
        return quotePerson;
    }

    public void setQuotePerson(String quotePerson) {
        this.quotePerson = quotePerson;
    }

    public String getQuoteOrg() {
        return quoteOrg;
    }

    public void setQuoteOrg(String quoteOrg) {
        this.quoteOrg = quoteOrg;
    }

    public String getQuoteDate() {
        return quoteDate;
    }

    public void setQuoteDate(String quoteDate) {
        this.quoteDate = quoteDate;
    }

    public BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    public void setQuoteVolume(BigDecimal quoteVolume) {
        this.quoteVolume = quoteVolume;
    }

    public String getQuoteDirection() {
        return quoteDirection;
    }

    public void setQuoteDirection(String quoteDirection) {
        this.quoteDirection = quoteDirection;
    }

    public String getSettleSpeed() {
        return settleSpeed;
    }

    public void setSettleSpeed(String settleSpeed) {
        this.settleSpeed = settleSpeed;
    }
}
