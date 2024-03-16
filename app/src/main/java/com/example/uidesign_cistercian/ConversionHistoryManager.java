package com.example.uidesign_cistercian;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConversionHistoryManager {

    private static ConversionHistoryManager instance; // Singleton instance
    private List<Integer> conversionHistory; // List to hold the history data
    private List<HistoryUpdateListener> listeners; // List to hold registered listeners
    private SharedPreferences sharedPreferences; // For persistent storage

    public interface HistoryUpdateListener {
        void onHistoryUpdated();
    }

    private ConversionHistoryManager(Context context) {
        conversionHistory = new ArrayList<>();
        listeners = new ArrayList<>();
        sharedPreferences = context.getSharedPreferences("ConversionHistoryPrefs", Context.MODE_PRIVATE);
        loadHistory(); // Load history from SharedPreferences
    }

    public static synchronized ConversionHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConversionHistoryManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadHistory() {
        String historyString = sharedPreferences.getString("history", null);
        if (historyString != null && !historyString.isEmpty()) {
            for (String itemStr : historyString.split(",")) {
                Log.d("HistoryDebug", "Adding item to history: " + Integer.parseInt(itemStr));
                conversionHistory.add(Integer.parseInt(itemStr));
            }
        }
    }

    private void saveHistory() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder historyString = new StringBuilder();
        for (int number : conversionHistory) {
            if (historyString.length() > 0) historyString.append(",");
            historyString.append(number);
        }
        editor.putString("history", historyString.toString());
        editor.apply();
    }

    // Method to get the conversion history
    public List<Integer> getConversionHistory() {
        return conversionHistory;
    }

    // Method to add a new entry to the history
    public void addConversion(int arabicNumber) {
        if (arabicNumber != 0) {
            conversionHistory.add(arabicNumber);
            saveHistory();
            notifyHistoryUpdated();
        }
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

    // Method for clearing the history
    public void clearHistory() {
        conversionHistory.clear(); // Clear the history list
        saveHistory(); // Persist the empty list to SharedPreferences
        notifyHistoryUpdated(); // Notify all listeners about the history update
    }

    public void removeConversions(Set<Integer> itemsToRemove) {
        conversionHistory.removeAll(itemsToRemove);
        saveHistory();
        notifyHistoryUpdated();
    }

    // Method to remove conversions by positions
    public void removeConversionsByPosition(List<Integer> positionsToRemove) {
        // Sort positions in reverse order to avoid shifting issues
        Collections.sort(positionsToRemove, Collections.reverseOrder());

        // Remove items from the end to avoid index shifting problems
        for (int position : positionsToRemove) {
            if (position >= 0 && position < conversionHistory.size()) {
                conversionHistory.remove(position);
            }
        }
        saveHistory();
        notifyHistoryUpdated();
    }
}