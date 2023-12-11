package com.example.uidesign_cistercian;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ListView historyListView = findViewById(R.id.historyListView);
        List<Integer> history = ConversionHistoryManager.getInstance().getConversionHistory();

        // Use an ArrayAdapter to show the history in the ListView
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, history);
        historyListView.setAdapter(adapter);
    }
}