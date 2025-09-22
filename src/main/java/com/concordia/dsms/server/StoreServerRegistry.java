package com.concordia.dsms.server;

import com.concordia.dsms.common.StoreServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StoreServerRegistry {
    private static final Map<String, StoreServer> CACHE = new ConcurrentHashMap<>();
    private static Registry registry;
    private static final List<String> STORE_CODES = List.of("QC", "ON", "BC");

    private StoreServerRegistry() {
    }

    public static void bind(String storeCode, StoreServer server) throws RemoteException {
        ensureRegistry();
        registry.rebind(storeCode, server);
        CACHE.put(storeCode, server);
    }

    public static StoreServer lookup(String storeCode) throws RemoteException {
        ensureRegistry();
        return CACHE.computeIfAbsent(storeCode, code -> {
            try {
                return (StoreServer) registry.lookup(code);
            } catch (RemoteException | NotBoundException e) {
                throw new RuntimeException("Unable to lookup store " + code, e);
            }
        });
    }

    public static List<String> getOtherStores(String currentStore) {
        return STORE_CODES.stream().filter(code -> !code.equals(currentStore)).toList();
    }

    private static void ensureRegistry() {
        if (registry != null) {
            return;
        }
        try {
            registry = LocateRegistry.getRegistry();
            registry.list();
        } catch (RemoteException e) {
            try {
                registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            } catch (RemoteException ex) {
                throw new RuntimeException("Unable to create RMI registry", ex);
            }
        }
    }
}