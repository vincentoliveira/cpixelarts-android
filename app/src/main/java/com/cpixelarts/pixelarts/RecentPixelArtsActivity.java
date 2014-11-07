package com.cpixelarts.pixelarts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.cpixelarts.pixelarts.R;
import com.cpixelarts.pixelarts.adapter.PixelArtAdapter;
import com.cpixelarts.pixelarts.model.PixelArt;
import com.cpixelarts.pixelarts.storage.PixelArtStorage;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.List;

public class RecentPixelArtsActivity extends Activity implements AdapterView.OnItemClickListener {

    private GridView gridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        gridView = (GridView) findViewById(R.id.grid_view);
        gridView.setOnItemClickListener(this);


        Tracker t = ((CPixelArtsApplication) getApplication()).getTracker();
        t.setScreenName("Recent Pixel Arts");
        t.send(new HitBuilders.EventBuilder().build());
    }

    // Refreshes the display if the network connection and the
    // pref settings allow it.
    @Override
    public void onStart() {
        super.onStart();

        loadPixelArts();
    }

    protected void loadPixelArts() {
        List<PixelArt> pixelArts = PixelArtStorage.loadRecentPixelArts(this);

        PixelArtAdapter adapter = new PixelArtAdapter(this, pixelArts);
        gridView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        PixelArt pixelArt = (PixelArt) gridView.getAdapter().getItem(position);

        Intent intent = new Intent(this, PixelArtActivity.class);
        intent.putExtra("id", pixelArt.id);
        intent.putExtra("title", pixelArt.title);
        intent.putExtra("width", pixelArt.width);
        intent.putExtra("height", pixelArt.height);
        startActivity(intent);
    }
}
