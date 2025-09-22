package com.concordia.dsms.server;

import com.concordia.dsms.common.PurchaseResult;
import com.concordia.dsms.common.StoreServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class StoreServerImpl extends UnicastRemoteObject implements StoreServer {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy", Locale.CANADA);

    private final String storeCode;
    private final Map<String, ItemRecord> inventory = new ConcurrentHashMap<>();
    // Wating list for each item
    private final Map<String, Deque<String>> waitLists = new ConcurrentHashMap<>();
    private final Logger logger;

    public StoreServerImpl(String storeCode) throws RemoteException {
        super();
        this.storeCode = Objects.requireNonNull(storeCode, "storeCode");
        this.logger = createLogger(storeCode);
    }

    private Logger createLogger(String storeCode) {
        Logger log = Logger.getLogger("DSMS-" + storeCode);
        log.setUseParentHandlers(false);
        try {
            FileHandler handler = new FileHandler("logs/" + storeCode + "_server.log", true);
            handler.setFormatter(new SimpleFormatter());
            log.addHandler(handler);
        } catch (Exception e) {
            throw new RuntimeException("Unable to initialize logger for store " + storeCode, e);
        }
        return log;
    }

    @Override
    public String addItem(String managerId, String itemId, String itemName, int quantity, double price) throws RemoteException {
        validateManager(managerId);
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(itemName, "itemName");
        if (!itemId.startsWith(storeCode)) {
            throw new RemoteException("Manager " + managerId + " cannot add items for store " + itemId.substring(0, 2));
        }
        if (quantity <= 0 || price <= 0) {
            throw new RemoteException("Quantity and price must be greater than zero.");
        }
        inventory.compute(itemId, (key, existing) -> {
            if (existing == null) {
                ItemRecord newRecord = new ItemRecord(itemId, itemName, quantity, price);
                logger.info(() -> String.format("Added new item %s (%s) qty=%d price=%.2f", itemId, itemName, quantity, price));
                return newRecord;
            }
            existing.getLock().lock();
            try {
                existing.increaseQuantity(quantity);
                int updatedQuantity = existing.getQuantity();
                logger.info(() -> String.format("Increased quantity for item %s by %d. New quantity=%d", itemId, quantity, updatedQuantity));
            } finally {
                existing.getLock().unlock();
            }
            return existing;
        });
        processWaitList(itemId);
        return "Item " + itemId + " successfully added/updated.";
    }

    @Override
    public String removeItem(String managerId, String itemId, int quantity) throws RemoteException {
        validateManager(managerId);
        Objects.requireNonNull(itemId, "itemId");
        if (!itemId.startsWith(storeCode)) {
            throw new RemoteException("Manager " + managerId + " cannot remove items for store " + itemId.substring(0, 2));
        }
        ItemRecord record = inventory.get(itemId);
        if (record == null) {
            return "Item " + itemId + " does not exist.";
        }
        record.getLock().lock();
        try {
            if (quantity <= 0 || quantity >= record.getQuantity()) {
                inventory.remove(itemId);
                waitLists.remove(itemId);
                logger.info(() -> String.format("Removed item %s from inventory.", itemId));
                return "Item " + itemId + " removed from inventory.";
            }
            record.decreaseQuantity(quantity);
            logger.info(() -> String.format("Decreased quantity for %s by %d. New quantity=%d", itemId, quantity, record.getQuantity()));
            return "Item " + itemId + " quantity decreased by " + quantity + ".";
        } finally {
            record.getLock().unlock();
        }
    }

    @Override
    public String listItemAvailability(String managerId) {
        validateManager(managerId);
        List<ItemRecord> records = new ArrayList<>(inventory.values());
        records.sort(Comparator.comparing(ItemRecord::getItemId));
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        for (ItemRecord record : records) {
            joiner.add(String.format("Item ID: %s, Item Name: %s, Item Quantity: %d, Item Price: %.2f",
                record.getItemId(), record.getItemName(), record.getQuantity(), record.getPrice()));
        }
        String result = joiner.length() == 0 ? "No items available." : joiner.toString();
        logger.info(() -> "Listing items for manager " + managerId + System.lineSeparator() + result);
        return result;
    }

    @Override
    public PurchaseResult purchaseItem(String customerId, String itemId, String dateOfPurchase) throws RemoteException {
        validateCustomer(customerId);
        LocalDate purchaseDate = parseDate(dateOfPurchase);
        String itemStore = itemId.substring(0, 2);
        if (itemStore.equals(storeCode)) {
            return performLocalPurchase(customerId, itemId, purchaseDate, false);
        }
        StoreServer remote = StoreServerRegistry.lookup(itemStore);
        double budget = CustomerAccountManager.getRemainingBudget(customerId);
        PurchaseResult result = remote.requestRemotePurchase(customerId, itemId, dateOfPurchase, budget);
        logger.info(() -> String.format("Forwarded purchase request for customer %s to store %s: %s", customerId, itemStore, result.getMessage()));
        return result;
    }

    @Override
    public String findItem(String customerId, String itemName) throws RemoteException {
        validateCustomer(customerId);
        StringBuilder builder = new StringBuilder();
        builder.append(searchLocalItems(itemName));
        for (String otherStore : StoreServerRegistry.getOtherStores(storeCode)) {
            StoreServer remote = StoreServerRegistry.lookup(otherStore);
            String remoteResponse = remote.requestRemoteItemLookup(itemName);
            if (!remoteResponse.isBlank()) {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(remoteResponse);
            }
        }
        String result = builder.length() == 0 ? "No items found." : builder.toString();
        logger.info(() -> "FindItem result for " + customerId + " item=" + itemName + System.lineSeparator() + result);
        return result;
    }

    @Override
    public String returnItem(String customerId, String itemId, String dateOfReturn) throws RemoteException {
        validateCustomer(customerId);
        LocalDate returnDate = parseDate(dateOfReturn);
        String itemStore = itemId.substring(0, 2);
        if (itemStore.equals(storeCode)) {
            return processReturn(customerId, itemId, returnDate);
        }
        StoreServer remote = StoreServerRegistry.lookup(itemStore);
        boolean accepted = remote.requestRemoteReturn(customerId, itemId, dateOfReturn);
        String message = accepted ? "Return processed by store " + itemStore : "Unable to return item " + itemId;
        logger.info(() -> String.format("Return request for %s forwarded to %s: %s", customerId, itemStore, message));
        return message;
    }

    @Override
    public PurchaseResult requestRemotePurchase(String customerId, String itemId, String dateOfPurchase, double budgetRemaining) throws RemoteException {
        LocalDate purchaseDate = parseDate(dateOfPurchase);
        PurchaseResult result = performLocalPurchase(customerId, itemId, purchaseDate, true);
        if (!result.isSuccess()) {
            logger.info(() -> String.format("Remote purchase failed for customer %s item %s: %s", customerId, itemId, result.getMessage()));
        }
        return result;
    }

    @Override
    public String requestRemoteItemLookup(String itemName) {
        return searchLocalItems(itemName);
    }

    @Override
    public boolean requestRemoteReturn(String customerId, String itemId, String dateOfReturn) throws RemoteException {
        LocalDate returnDate = parseDate(dateOfReturn);
        String response = processReturn(customerId, itemId, returnDate);
        return response.toLowerCase(Locale.CANADA).contains("success");
    }

    private PurchaseResult performLocalPurchase(String customerId, String itemId, LocalDate purchaseDate, boolean fromRemote) {
        ItemRecord record = inventory.get(itemId);
        if (record == null) {
            waitLists.computeIfAbsent(itemId, key -> new LinkedList<>());
            return new PurchaseResult(false, "Item " + itemId + " is not available.", 0);
        }
        record.getLock().lock();
        try {
            if (record.getQuantity() <= 0) {
                waitLists.computeIfAbsent(itemId, key -> new LinkedList<>()).add(customerId);
                logger.info(() -> String.format("Customer %s added to waitlist for item %s", customerId, itemId));
                return new PurchaseResult(false, "Item unavailable. Added to waitlist.", 0);
            }
            double price = record.getPrice();
            boolean purchaseRecorded = CustomerAccountManager.attemptPurchase(customerId, storeCode, itemId, price, purchaseDate);
            if (!purchaseRecorded) {
                return new PurchaseResult(false, "Purchase denied due to budget or policy limits.", 0);
            }
            record.decreaseQuantity(1);
            logger.info(() -> String.format("Customer %s purchased item %s for %.2f", customerId, itemId, price));
            return new PurchaseResult(true, "Purchase successful for item " + itemId, price);
        } finally {
            record.getLock().unlock();
        }
    }

    private String searchLocalItems(String itemName) {
        StringJoiner joiner = new StringJoiner(System.lineSeparator());
        inventory.values().stream()
                .filter(record -> record.getItemName().equalsIgnoreCase(itemName))
                .sorted(Comparator.comparing(ItemRecord::getItemId))
                .forEach(record -> joiner.add(String.format("%s %d %.2f", record.getItemId(), record.getQuantity(), record.getPrice())));
        return joiner.toString();
    }

    private String processReturn(String customerId, String itemId, LocalDate returnDate) {
        ItemRecord record = inventory.get(itemId);
        if (record == null) {
            return "Item " + itemId + " does not belong to store " + storeCode;
        }
        Optional<PurchaseRecord> purchaseRecordOpt = CustomerAccountManager.consumePurchaseRecord(customerId, itemId);
        if (purchaseRecordOpt.isEmpty()) {
            return "No purchase record found for item " + itemId;
        }
        PurchaseRecord purchaseRecord = purchaseRecordOpt.get();
        if (purchaseRecord.purchaseDate().plusDays(30).isBefore(returnDate)) {
            CustomerAccountManager.restorePurchaseRecord(customerId, purchaseRecord);
            return "Return period expired for item " + itemId;
        }
        record.getLock().lock();
        try {
            record.increaseQuantity(1);
            CustomerAccountManager.refund(customerId, purchaseRecord.price());
            logger.info(() -> String.format("Customer %s returned item %s", customerId, itemId));
        } finally {
            record.getLock().unlock();
        }
        processWaitList(itemId);
        return "Return successful for item " + itemId;
    }

    private void processWaitList(String itemId) {
        Deque<String> queue = waitLists.get(itemId);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        ItemRecord record = inventory.get(itemId);
        if (record == null) {
            waitLists.remove(itemId);
            return;
        }
        while (record.getQuantity() > 0 && !queue.isEmpty()) {
            String customerId = queue.poll();
            if (customerId == null) {
                continue;
            }
            PurchaseResult result = performLocalPurchase(customerId, itemId, LocalDate.now(), true);
            if (result.isSuccess()) {
                logger.info(() -> String.format("Waitlisted customer %s automatically purchased %s", customerId, itemId));
            } else {
                logger.info(() -> String.format("Waitlisted purchase for %s failed: %s", customerId, result.getMessage()));
            }
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date format, expected ddMMyyyy", ex);
        }
    }

    private void validateManager(String managerId) {
        if (managerId == null || managerId.length() < 3 || !managerId.startsWith(storeCode + "M")) {
            throw new IllegalArgumentException("Manager " + managerId + " is not authorized for store " + storeCode);
        }
    }

    private void validateCustomer(String customerId) {
        if (customerId == null || customerId.length() < 3 || !customerId.startsWith(storeCode + "U")) {
            throw new IllegalArgumentException("Customer " + customerId + " must interact with home store " + storeCode);
        }
    }

}

