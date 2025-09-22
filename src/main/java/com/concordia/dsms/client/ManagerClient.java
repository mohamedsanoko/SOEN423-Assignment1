package com.concordia.dsms.client;

import com.concordia.dsms.common.StoreServer;
import com.concordia.dsms.server.StoreServerRegistry;

import java.rmi.RemoteException;
import java.util.Scanner;

public final class ManagerClient {
    private ManagerClient() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: ManagerClient <managerId>");
            return;
        }

        String managerId = args[0].toUpperCase();
        String storeCode = managerId.substring(0, 2);
        ClientLogger logger = new ClientLogger(managerId);
        StoreServer server = StoreServerRegistry.lookup(storeCode);
        logger.info("Connected to store " + storeCode);
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                System.out.println("\nManager Menu");
                System.out.println("1. Add Item");
                System.out.println("2. Remove Item");
                System.out.println("3. List Item Availability");
                System.out.println("4. Exit");
                System.out.print("Select an option: ");
                String choice = scanner.nextLine().trim();
                try {
                    switch (choice) {
                        case "1" -> handleAddItem(scanner, server, managerId, logger);
                        case "2" -> handleRemoveItem(scanner, server, managerId, logger);
                        case "3" -> handleListItems(server, managerId, logger);
                        case "4" -> running = false;
                        default -> System.out.println("Invalid option. Try again.");
                    }
                } catch (Exception e) {
                    logger.error("Error executing manager operation", e);
                    System.out.println("Operation failed: " + e.getMessage());
                }
            }
        }
    }

    private static void handleAddItem(Scanner scanner, StoreServer server, String managerId, ClientLogger logger) throws RemoteException {
        System.out.print("Item ID: ");
        String itemId = scanner.nextLine().trim().toUpperCase();
        System.out.print("Item Name: ");
        String itemName = scanner.nextLine().trim();
        System.out.print("Quantity: ");
        int quantity = Integer.parseInt(scanner.nextLine().trim());
        System.out.print("Price: ");
        double price = Double.parseDouble(scanner.nextLine().trim());
        String response = server.addItem(managerId, itemId, itemName, quantity, price);
        logger.info("addItem -> " + response);
        System.out.println(response);
    }

    private static void handleRemoveItem(Scanner scanner, StoreServer server, String managerId, ClientLogger logger) throws RemoteException {
        System.out.print("Item ID: ");
        String itemId = scanner.nextLine().trim().toUpperCase();
        System.out.print("Quantity to remove (0 for complete removal): ");
        int quantity = Integer.parseInt(scanner.nextLine().trim());
        String response = server.removeItem(managerId, itemId, quantity);
        logger.info("removeItem -> " + response);
        System.out.println(response);
    }

    private static void handleListItems(StoreServer server, String managerId, ClientLogger logger) throws RemoteException {
        String response = server.listItemAvailability(managerId);
        logger.info("listItemAvailability -> " + response.replace(System.lineSeparator(), " | "));
        System.out.println(response);
    }
}