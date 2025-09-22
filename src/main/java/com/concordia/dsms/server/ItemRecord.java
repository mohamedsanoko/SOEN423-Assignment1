package com.concordia.dsms.server;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

class ItemRecord implements Serializable {
    private final String itemId;
    private final String itemName;
    private final double price;
    private int quantity;
    private final ReentrantLock lock = new ReentrantLock(true);

    ItemRecord(String itemId, String itemName, int quantity, double price) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

    String getItemId() {
        return itemId;
    }

    String getItemName() {
        return itemName;
    }

    double getPrice() {
        return price;
    }

    int getQuantity() {
        return quantity;
    }

    void increaseQuantity(int delta) {
        quantity += delta;
    }

    void decreaseQuantity(int delta) {
        quantity -= delta;
    }

    ReentrantLock getLock() {
        return lock;
    }
}
