package com.example.uidesign_cistercian;

import java.util.ArrayList;
import java.util.List;

public class ConversionHistoryManager {

    private static ConversionHistoryManager instance; // Singleton instance
    private List<Integer> conversionHistory; // List to hold the history data
    private List<HistoryUpdateListener> listeners; // List to hold registered listeners

    public interface HistoryUpdateListener {
        void onHistoryUpdated();
    }

    private ConversionHistoryManager() {
        conversionHistory = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public static synchronized ConversionHistoryManager getInstance() {
        if (instance == null) {
            instance = new ConversionHistoryManager();
        }
        return instance;
    }

    // Method to get the conversion history
    public List<Integer> getConversionHistory() {
        return conversionHistory;
    }

    // Method to add a new entry to the history
    public void addConversion(int arabicNumber) {
        conversionHistory.add(arabicNumber);
        notifyHistoryUpdated();
    }

    // Method to register a listener
    public void addHistoryUpdateListener(HistoryUpdateListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // Method to unregister a listener
    public void removeHistoryUpdateListener(HistoryUpdateListener listener) {
        listeners.remove(listener);
    }

    // Method to notify all registered listeners of an update
    private void notifyHistoryUpdated() {
        for (HistoryUpdateListener listener : listeners) {
            listener.onHistoryUpdated();
        }
    }
}