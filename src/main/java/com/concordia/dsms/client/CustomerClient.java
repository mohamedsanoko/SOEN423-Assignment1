package com.concordia.dsms.client;

import com.concordia.dsms.common.PurchaseResult;
import com.concordia.dsms.common.StoreServer;
import com.concordia.dsms.server.StoreServerRegistry;

import java.util.Scanner;

public class CustomerClient {
    private CustomerClient() {

    }

    public static void main(String[] args) throws Exception{
        if (args.length == 0) {
            System.err.println("Usage: CustomerClient <customerId>");
            return;
        }

        String customerId = args[0].toUpperCase();
        String storeCode = customerId.substring(0, 2);
        ClientLogger logger = new ClientLogger(customerId);
        StoreServer server = StoreServerRegistry.lookup(storeCode);
        logger.info("Connected to home store " + storeCode);
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println("\nCustomer Menu");
                System.out.println("1. Purchase Item");
                System.out.println("2. Find Item");
                System.out.println("3. Return Item");
                System.out.println("4. Exit");
                System.out.print("Select an option: ");
                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> handlePurchase(scanner, server, customerId, logger);
                        case "2" -> handleFind(scanner, server, customerId, logger);
                        case "3" -> handleReturn(scanner, server, customerId, logger);
                        case "4" -> running = false;
                        default -> System.out.println("Invalid option. Try again.");
                    }
                } catch (Exception e) {
                    logger.error("Error executing customer operation", e);
                    System.out.println("Operation failed: " + e.getMessage());
                }
            }
        }
    }

    private static void handlePurchase(Scanner scanner, StoreServer server, String customerId, ClientLogger logger) throws Exception {
        System.out.print("Item ID: ");
        String itemId = scanner.nextLine().trim().toUpperCase();
        System.out.print("Date (ddMMyyyy): ");
        String date = scanner.nextLine().trim();
        PurchaseResult result = server.purchaseItem(customerId, itemId, date);
        logger.info("purchaseItem -> " + result.getMessage());
        System.out.println(result.getMessage());
    }

    private static void handleFind(Scanner scanner, StoreServer server, String customerId, ClientLogger logger) throws Exception {
        System.out.print("Item Name: ");
        String itemName = scanner.nextLine().trim();
        String result = server.findItem(customerId, itemName);
        logger.info("findItem -> " + result.replace(System.lineSeparator(), " | "));
        System.out.println(result);
    }

    private static void handleReturn(Scanner scanner, StoreServer server, String customerId, ClientLogger logger) throws Exception {
        System.out.print("Item ID: ");
        String itemId = scanner.nextLine().trim().toUpperCase();
        System.out.print("Date (ddMMyyyy): ");
        String date = scanner.nextLine().trim();
        String result = server.returnItem(customerId, itemId, date);
        logger.info("returnItem -> " + result);
        System.out.println(result);
    }
}