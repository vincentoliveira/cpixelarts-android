package com.cpixelarts.pixelarts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.cpixelarts.pixelarts.adapter.PixelArtAdapter;
import com.cpixelarts.pixelarts.model.PixelArt;
import com.cpixelarts.pixelarts.storage.GalleryStorage;
import com.cpixelarts.pixelarts.utils.ImageManager;
import com.cpixelarts.pixelarts.utils.PixelArtParser;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


public class GalleryActivity extends Activity implements AdapterView.OnItemClickListener {
    public static final String BASE_URL = "http://cpixelarts.com";
    private GridView gridView;
    private static final String URL = "/api/drawings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        gridView = (GridView) findViewById(R.id.grid_view);
        gridView.setOnItemClickListener(this);

        // Use 1/8th of the available memory for this memory cache.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        ImageManager.getInstance().init(cacheSize);

        String galleryJson = GalleryStorage.loadGallery(this);
        handleGalleryJson(galleryJson);

        Tracker t = ((CPixelArtsApplication) getApplication()).getTracker();
        t.setScreenName("Gallery");
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    protected boolean handleGalleryJson(String galleryJson) {
        List<PixelArt> pixelArts = null;
        if (galleryJson != null) {
            pixelArts = PixelArtParser.parseList(galleryJson);
        }

        if (pixelArts == null || pixelArts.isEmpty()) {
            return false;
        }

        PixelArtAdapter adapter = new PixelArtAdapter(this, pixelArts);
        gridView.setAdapter(adapter);

        return true;
    }

    // Refreshes the display if the network connection and the
    // pref settings allow it.
    @Override
    public void onStart() {
        super.onStart();

        loadPixelArts();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_create) {
            Intent intent = new Intent(this, PixelArtActivity.class);
            intent.putExtra("new", true);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_my_pixel_arts) {
            Intent intent = new Intent(this, RecentPixelArtsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Uses AsyncTask subclass to download the pixel arts from api
    private void loadPixelArts() {
        new DownloadPixelArtsTask().execute(URL);
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

    private class DownloadPixelArtsTask extends AsyncTask<String, Void, String> {
        private static final String errorMsg = "Unable to retrieve pixel arts.";

        @Override
        protected String doInBackground(String... urls) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                String url = GalleryActivity.BASE_URL + urls[0];
                response = httpclient.execute(new HttpGet(url));
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    return out.toString();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
                e.printStackTrace();
            } catch (IOException e) {
                //TODO Handle problems..
                e.printStackTrace();
            }

            return errorMsg;
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.equals(errorMsg) && handleGalleryJson(result)) {
                GalleryStorage.storeGallery(getBaseContext(), result);
            } else {
                Toast.makeText(getBaseContext(), R.string.msg_unable_retrieve_pixel_arts, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
