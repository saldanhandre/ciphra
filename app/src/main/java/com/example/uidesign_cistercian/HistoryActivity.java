package com.example.uidesign_cistercian;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.MenuItem;
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
        ConversionHistoryManager.getInstance().addHistoryUpdateListener(this);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initial history update
        updateHistory();
    }

    @Override
    public void onHistoryUpdated() {
        updateHistory();
    }

    // Method to update history and refresh the ListView
    public void updateHistory() {
        List<Integer> history = ConversionHistoryManager.getInstance().getConversionHistory();
        Collections.reverse(history); // Reverse the list if required

        if(adapter == null) {
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
        // Handle the Up button click here
        if (item.getItemId() == android.R.id.home) {
            // Finish this activity and return to the parent activity (MainActivity)
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ConversionHistoryManager.getInstance().removeHistoryUpdateListener(this);
    }
}
