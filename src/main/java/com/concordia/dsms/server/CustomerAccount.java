package com.concordia.dsms.server;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CustomerAccount implements Serializable {
    private static final double DEFAULT_BUDGET = 1000.0;

    private final String customerId;
    private double remainingBudget = DEFAULT_BUDGET;
    private final Map<String, Integer> purchasesPerStore = new HashMap<>();
    private final Map<String, List<PurchaseRecord>> purchasesByItem = new HashMap<>();

    CustomerAccount(String customerId) {
        this.customerId = customerId;
    }

    synchronized double getRemainingBudget() {
        return remainingBudget;
    }

    synchronized boolean attemptPurchase(String storeCode, String itemId, double price, LocalDate date) {
        if (!storeCode.equals(getHomeStore()) && purchasesPerStore.getOrDefault(storeCode, 0) >= 1) {
            return false;
        }
        if (remainingBudget < price) {
            return false;
        }
        purchasesPerStore.merge(storeCode, 1, Integer::sum);
        remainingBudget -= price;
        purchasesByItem.computeIfAbsent(itemId, key -> new ArrayList<>()).add(new PurchaseRecord(itemId, storeCode, date, price));
        return true;
    }

    synchronized boolean hasPurchaseRecord(String itemId) {
        return purchasesByItem.containsKey(itemId) && !purchasesByItem.get(itemId).isEmpty();
    }

    // Method for when a customer returns an item
    synchronized PurchaseRecord consumePurchaseRecord(String itemId) {
        List<PurchaseRecord> records = purchasesByItem.get(itemId);
        if (records == null || records.isEmpty()) {
            return null;
        }
        PurchaseRecord record = records.remove(0);
        purchasesPerStore.merge(record.storeCode(), -1, Integer::sum);
        if (purchasesPerStore.get(record.storeCode()) <= 0) {
            purchasesPerStore.remove(record.storeCode());
        }
        remainingBudget += record.price();
        return record;
    }

    // Re-applies a purchase record, updates store counts, and deducts the money again
    synchronized void restorePurchaseRecord(PurchaseRecord record) {
        purchasesPerStore.merge(record.storeCode(), 1, Integer::sum);
        purchasesByItem.computeIfAbsent(record.itemId(), key -> new ArrayList<>()).add(0, record);
        remainingBudget -= record.price();
    }

    synchronized void refund(double price) {
        remainingBudget += price;
    }

    // Return how many purchases a customer has from a specific remote store
    synchronized int getRemotePurchaseCount(String storeCode) {
        return purchasesPerStore.getOrDefault(storeCode, 0);
    }

    String getCustomerId() {
        return customerId;
    }

    private String getHomeStore() {
        return customerId.substring(0, 2);
    }
}
