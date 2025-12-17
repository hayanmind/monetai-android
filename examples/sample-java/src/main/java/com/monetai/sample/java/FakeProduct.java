package com.monetai.sample.java;

public class FakeProduct {
    public final String id;
    public final String title;
    public final String description;
    public final double price;
    public final double regularPrice;
    public final String currencyCode;
    public final Integer month; // nullable

    public FakeProduct(String id, String title, String description, double price, double regularPrice, String currencyCode, Integer month) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.regularPrice = regularPrice;
        this.currencyCode = currencyCode;
        this.month = month;
    }
}

