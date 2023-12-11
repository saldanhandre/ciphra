package com.example.uidesign_cistercian;

import java.util.ArrayList;
import java.util.List;

public class ConversionHistoryManager {
    private static ConversionHistoryManager instance;

    private List<Integer> conversionHistory;

    private ConversionHistoryManager() {
        conversionHistory = new ArrayList<>();
    }

    public static synchronized ConversionHistoryManager getInstance() {
        if (instance == null) {
            instance = new ConversionHistoryManager();
        }
        return instance;
    }

    public void addConversion(int arabicNumber) {
        conversionHistory.add(arabicNumber);
    }

    public List<Integer> getConversionHistory() {
        return conversionHistory;
    }

    public void clearHistory() {
        conversionHistory.clear();
    }
}