package com.concordia.dsms.server;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class CustomerAccountManager {
    // In-memory database of all customers in the system
    private static final Map<String, CustomerAccount> ACCOUNTS = new ConcurrentHashMap<>();

    private CustomerAccountManager() {
    }

    static CustomerAccount getAccount(String customerId) {
        return ACCOUNTS.computeIfAbsent(customerId, CustomerAccount::new);
    }

    static double getRemainingBudget(String customerId) {
        return getAccount(customerId).getRemainingBudget();
    }

    static boolean attemptPurchase(String customerId, String storeCode, String itemId, double price, LocalDate date) {
        return getAccount(customerId).attemptPurchase(storeCode, itemId, price, date);
    }

    // Removes a specific purchase record from a cusomter's account
    static Optional<PurchaseRecord> consumePurchaseRecord(String customerId, String itemId) {
        CustomerAccount account = ACCOUNTS.get(customerId);
        if (account == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(account.consumePurchaseRecord(itemId));
    }

    static void restorePurchaseRecord(String customerId, PurchaseRecord record) {
        getAccount(customerId).restorePurchaseRecord(record);
    }

    static void refund(String customerId, double price) {
        getAccount(customerId).refund(price);
    }
}
