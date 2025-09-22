package com.concordia.dsms.server;

import com.concordia.dsms.common.PurchaseResult;
import com.concordia.dsms.common.StoreServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;

import static org.junit.jupiter.api.Assertions.*;

class StoreServerImplTest {

    @BeforeAll
    static void setupRegistry() throws Exception {
        StoreServer qc = new StoreServerImpl("QC");
        StoreServer on = new StoreServerImpl("ON");
        StoreServer bc = new StoreServerImpl("BC");
        StoreServerRegistry.bind("QC", qc);
        StoreServerRegistry.bind("ON", on);
        StoreServerRegistry.bind("BC", bc);
    }

    @Test
    void testAddPurchaseAndReturnFlow() throws Exception {
        StoreServer server = StoreServerRegistry.lookup("QC");
        String managerId = "QCM0001";
        String itemId = "QC5555";
        server.addItem(managerId, itemId, "TestItem", 2, 50.0);

        PurchaseResult firstPurchase = server.purchaseItem("QCU1234", itemId, "01012025");
        assertTrue(firstPurchase.isSuccess(), "First purchase should succeed");

        PurchaseResult secondPurchase = server.purchaseItem("QCU1235", itemId, "02012025");
        assertTrue(secondPurchase.isSuccess(), "Second purchase should succeed");

        PurchaseResult waitlistPurchase = server.purchaseItem("QCU1236", itemId, "03012025");
        assertFalse(waitlistPurchase.isSuccess(), "Third purchase should trigger waitlist");

        String returnResult = server.returnItem("QCU1234", itemId, "05012025");
        assertTrue(returnResult.toLowerCase().contains("successful"), "Return should be successful");
    }

    @Test
    void testFindItem() throws RemoteException {
        StoreServer server = StoreServerRegistry.lookup("QC");
        String managerId = "QCM0001";
        String itemId = "QC9999";
        server.addItem(managerId, itemId, "TestItem", 1, 50.0);

        String result = server.findItem("QCU1234", "TestItem");
        assertFalse(result.isBlank(), "FindItem should return results");
    }
}
