package com.concordia.dsms.server;

import java.io.Serializable;
import java.time.LocalDate;

public final class PurchaseRecord implements Serializable {
    private final String itemId;
    private final String storeCode;
    private final LocalDate purchaseDate;
    private final double price;

    public PurchaseRecord(String itemId, String storeCode, LocalDate purchaseDate, double price) {
        this.itemId = itemId;
        this.storeCode = storeCode;
        this.purchaseDate = purchaseDate;
        this.price = price;
    }

    public String itemId() {
        return itemId;
    }

    public String storeCode() {
        return storeCode;
    }

    public LocalDate purchaseDate() {
        return purchaseDate;
    }

    public double price() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PurchaseRecord that = (PurchaseRecord) o;
        return Double.compare(that.price, price) == 0 &&
               itemId.equals(that.itemId) &&
               storeCode.equals(that.storeCode) &&
               purchaseDate.equals(that.purchaseDate);
    }

    @Override
    public int hashCode() {
        int result = itemId.hashCode();
        result = 31 * result + storeCode.hashCode();
        result = 31 * result + purchaseDate.hashCode();
        result = 31 * result + Double.hashCode(price);
        return result;
    }

    @Override
    public String toString() {
        return "PurchaseRecord[" +
               "itemId=" + itemId + ", " +
               "storeCode=" + storeCode + ", " +
               "purchaseDate=" + purchaseDate + ", " +
               "price=" + price + ']';
    }
}