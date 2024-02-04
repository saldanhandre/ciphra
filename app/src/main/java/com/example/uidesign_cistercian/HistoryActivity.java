package com.example.uidesign_cistercian;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class HistoryActivity extends AppCompatActivity implements ConversionHistoryManager.HistoryUpdateListener {

    private ListView historyListView;
    private HistoryItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        historyListView = findViewById(R.id.historyListView);

        // Register as a listener
        ConversionHistoryManager.getInstance(getApplicationContext()).addHistoryUpdateListener(this);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initial history update
        updateHistory();

        // Set an onItemClickListener to the ListView
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected number from the adapter
                int selectedNumber = adapter.getItem(position);

                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                intent.putExtra("selectedNumber", selectedNumber);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent); // Directly use startActivity here
            }
        });
    }

    @Override
    public void onHistoryUpdated() {
        runOnUiThread(this::updateHistory);
    }

    // Method to update history and refresh the ListView
    public void updateHistory() {
        List<Integer> history = new ArrayList<>(ConversionHistoryManager.getInstance(getApplicationContext()).getConversionHistory());
        Collections.reverse(history);

        if (adapter == null) {
            adapter = new HistoryItemAdapter(this, history);
            historyListView.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(history);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Finish this activity and return to the MainActivity
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConversionHistoryManager.getInstance(getApplicationContext()).removeHistoryUpdateListener(this);
    }
}
