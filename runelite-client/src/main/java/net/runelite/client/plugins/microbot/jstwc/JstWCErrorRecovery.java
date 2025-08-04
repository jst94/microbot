package net.runelite.client.plugins.microbot.jstwc;

import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.jstwc.enums.JstWCState;

import java.util.HashMap;
import java.util.Map;

public class JstWCErrorRecovery {
    
    private final Map<JstWCState, Integer> retryAttempts = new HashMap<>();
    private final Map<JstWCState, Long> lastAttemptTime = new HashMap<>();
    private final Map<String, Integer> itemPurchaseAttempts = new HashMap<>();
    
    private JstWCState previousState = null;
    private JstWCState stateBeforeError = null;
    private String lastError = "";
    private long disconnectTime = 0;
    
    // Configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MAX_ITEM_PURCHASE_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds between retries
    private static final long DISCONNECT_TIMEOUT_MS = 30000; // 30 seconds to recover from disconnect
    
    public boolean shouldRetry(JstWCState currentState) {
        int attempts = retryAttempts.getOrDefault(currentState, 0);
        long lastAttempt = lastAttemptTime.getOrDefault(currentState, 0L);
        long timeSinceLastAttempt = System.currentTimeMillis() - lastAttempt;
        
        return attempts < MAX_RETRY_ATTEMPTS && timeSinceLastAttempt >= RETRY_DELAY_MS;
    }
    
    public void recordRetryAttempt(JstWCState state) {
        retryAttempts.put(state, retryAttempts.getOrDefault(state, 0) + 1);
        lastAttemptTime.put(state, System.currentTimeMillis());
        Microbot.log("Retry attempt " + retryAttempts.get(state) + "/" + MAX_RETRY_ATTEMPTS + " for state: " + state);
    }
    
    public void recordSuccess(JstWCState state) {
        retryAttempts.remove(state);
        lastAttemptTime.remove(state);
        if (state == stateBeforeError) {
            stateBeforeError = null;
            lastError = "";
        }
    }
    
    public void recordError(JstWCState state, String error) {
        if (stateBeforeError == null) {
            stateBeforeError = state;
        }
        lastError = error;
        Microbot.log("Error in state " + state + ": " + error);
    }
    
    public boolean shouldRetryItemPurchase(String itemName) {
        int attempts = itemPurchaseAttempts.getOrDefault(itemName, 0);
        return attempts < MAX_ITEM_PURCHASE_ATTEMPTS;
    }
    
    public void recordItemPurchaseAttempt(String itemName) {
        itemPurchaseAttempts.put(itemName, itemPurchaseAttempts.getOrDefault(itemName, 0) + 1);
        Microbot.log("Purchase attempt " + itemPurchaseAttempts.get(itemName) + "/" + MAX_ITEM_PURCHASE_ATTEMPTS + " for item: " + itemName);
    }
    
    public void recordItemPurchaseSuccess(String itemName) {
        itemPurchaseAttempts.remove(itemName);
    }
    
    public void clearItemPurchaseAttempts() {
        itemPurchaseAttempts.clear();
    }
    
    public void recordDisconnect() {
        disconnectTime = System.currentTimeMillis();
        Microbot.log("Disconnection detected at " + disconnectTime);
    }
    
    public boolean isRecoveringFromDisconnect() {
        if (disconnectTime == 0) return false;
        
        long timeSinceDisconnect = System.currentTimeMillis() - disconnectTime;
        if (timeSinceDisconnect > DISCONNECT_TIMEOUT_MS) {
            disconnectTime = 0; // Reset after timeout
            return false;
        }
        return true;
    }
    
    public void clearDisconnect() {
        disconnectTime = 0;
    }
    
    public JstWCState getRecoveryState(JstWCState currentState) {
        // If we were in an equipment-related state, start from checking equipment
        if (isEquipmentRelatedState(currentState)) {
            return JstWCState.CHECKING_EQUIPMENT;
        }
        
        // If we were traveling, check if we're at location
        if (currentState == JstWCState.TRAVELING_TO_LOCATION) {
            return JstWCState.INITIALIZING;
        }
        
        // For other states, return to initialization
        return JstWCState.INITIALIZING;
    }
    
    private boolean isEquipmentRelatedState(JstWCState state) {
        return state == JstWCState.CHECKING_EQUIPMENT ||
               state == JstWCState.BANKING_FOR_EQUIPMENT ||
               state == JstWCState.BUYING_EQUIPMENT ||
               state == JstWCState.COLLECTING_PURCHASES ||
               state == JstWCState.PREPARING_FOR_TRAVEL;
    }
    
    public void reset() {
        retryAttempts.clear();
        lastAttemptTime.clear();
        itemPurchaseAttempts.clear();
        previousState = null;
        stateBeforeError = null;
        lastError = "";
        disconnectTime = 0;
    }
    
    public void updatePreviousState(JstWCState state) {
        if (state != JstWCState.ERROR && state != JstWCState.RETRYING) {
            previousState = state;
        }
    }
    
    public JstWCState getPreviousState() {
        return previousState;
    }
    
    public JstWCState getStateBeforeError() {
        return stateBeforeError;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public boolean hasExceededRetries(JstWCState state) {
        return retryAttempts.getOrDefault(state, 0) >= MAX_RETRY_ATTEMPTS;
    }
    
    public int getRemainingRetries(JstWCState state) {
        return MAX_RETRY_ATTEMPTS - retryAttempts.getOrDefault(state, 0);
    }
}