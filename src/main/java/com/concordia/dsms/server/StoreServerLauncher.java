package com.concordia.dsms.server;

import com.concordia.dsms.common.StoreServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.Map;

public final class StoreServerLauncher {
    private StoreServerLauncher() {
    }

    public static void main(String[] args) {
        try {
            Files.createDirectories(Path.of("logs"));
            StoreServer qcServer = new StoreServerImpl("QC");
            StoreServer onServer = new StoreServerImpl("ON");
            StoreServer bcServer = new StoreServerImpl("BC");

            StoreServerRegistry.bind("QC", qcServer);
            StoreServerRegistry.bind("ON", onServer);
            StoreServerRegistry.bind("BC", bcServer);

            loadInitialData();
            System.out.println("DSMS servers started successfully. Press Ctrl+C to exit.");
        } catch (Exception e) {
            throw new RuntimeException("Unable to start DSMS servers", e);
        }
    }

    private static void loadInitialData() throws RemoteException {
        Map.of(
                "QC", new String[][]{
                        {"QC1001", "Laptop", "5", "900"},
                        {"QC1002", "Headphones", "10", "150"}
                },
                "ON", new String[][]{
                        {"ON2001", "Camera", "4", "550"},
                        {"ON2002", "Coffee Maker", "6", "120"}
                },
                "BC", new String[][]{
                        {"BC3001", "Bicycle", "3", "400"},
                        {"BC3002", "Backpack", "8", "80"}
                }
        ).forEach((storeCode, items) -> {
            for (String[] item : items) {
                try {
                    String managerId = storeCode + "M0000";
                    StoreServerRegistry.lookup(storeCode).addItem(
                            managerId,
                            item[0],
                            item[1],
                            Integer.parseInt(item[2]),
                            Double.parseDouble(item[3])
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Unable to load initial data for store " + storeCode, e);
                }
            }
        });
    }
}