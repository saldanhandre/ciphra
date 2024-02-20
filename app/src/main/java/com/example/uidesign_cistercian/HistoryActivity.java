package com.example.uidesign_cistercian;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
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
                startActivity(intent);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_delete_history) {
            // Handle delete history action
            deleteHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConversionHistoryManager.getInstance(getApplicationContext()).removeHistoryUpdateListener(this);
    }

    private void deleteHistory() {
        // Call clearHistory on your singleton instance of ConversionHistoryManager
        ConversionHistoryManager.getInstance(getApplicationContext()).clearHistory();
        // Update the UI by refreshing the history list
        updateHistory();
    }
}
