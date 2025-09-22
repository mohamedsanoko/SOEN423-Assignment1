package com.concordia.dsms.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StoreServer extends Remote {
    String addItem(String managerId, String itemId, String itemName, int quantity, double price) throws RemoteException;

    String removeItem(String managerId, String itemId, int quantity) throws RemoteException;

    String listItemAvailability(String managerId) throws RemoteException;

    PurchaseResult purchaseItem(String customerId, String itemId, String dateOfPurchase) throws RemoteException;

    String findItem(String customerId, String itemName) throws RemoteException;

    String returnItem(String customerId, String itemId, String dateOfReturn) throws RemoteException;

    PurchaseResult requestRemotePurchase(String customerId, String itemId, String dateOfPurchase, double budgetRemaining) throws RemoteException;

    String requestRemoteItemLookup(String itemName) throws RemoteException;

    boolean requestRemoteReturn(String customerId, String itemId, String dateOfReturn) throws RemoteException;
}
