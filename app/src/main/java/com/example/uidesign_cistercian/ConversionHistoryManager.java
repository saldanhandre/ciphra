package com.example.uidesign_cistercian;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

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
        conversionHistory.add(arabicNumber);
        saveHistory();
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