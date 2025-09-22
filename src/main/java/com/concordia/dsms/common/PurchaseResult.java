package com.concordia.dsms.common;

import java.io.Serializable;

public class PurchaseResult implements Serializable{
    private final boolean success;
    private final String message;
    private final double priceCharged;

    public PurchaseResult(boolean success, String message, double priceCharged) {
        this.success = success;
        this.message = message;
        this.priceCharged = priceCharged;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public double getPriceCharged() {
        return priceCharged;
    }

    @Override
    public String toString() {
        return "PurchaseResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", priceCharged=" + priceCharged +
                '}';
    }
}
