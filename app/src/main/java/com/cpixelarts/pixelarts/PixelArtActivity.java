package com.cpixelarts.pixelarts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.cpixelarts.pixelarts.model.PixelArt;
import com.cpixelarts.pixelarts.restclient.PatchAsyncTask;
import com.cpixelarts.pixelarts.storage.PixelArtStorage;
import com.cpixelarts.pixelarts.restclient.GetAsyncTask;
import com.cpixelarts.pixelarts.utils.PixelArtParser;
import com.cpixelarts.pixelarts.restclient.PostAsyncTask;
import com.cpixelarts.pixelarts.view.PixelArtView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PixelArtActivity extends Activity implements PixelArtView.OnSelectPixelsListener {
    private static final String VIEW_PIXEL_ART_URL = "/drawing/{title}";
    private static final String PIXEL_ART_FILE_URL = "/gallery/drawing-{title}_512.png";
    private static final String PIXEL_ART_URL = "/api/drawings/{id}";
    private static final String CREATE_PIXEL_ART_URL = "/api/drawings";
    private static final String ADD_PIXEL_URL = "/api/drawings/{id}/pixels";
    private static final String SET_TITLE_URL = "/api/drawings/{id}/title";
    private static final String LOCK_URL = "/api/drawings/{id}/lock";

    private static final String EVENT_ADD_PIXEL_CATEGORY = "pixel";
    private static final String EVENT_ADD_PIXEL_ACTION = "add";
    private static final String EVENT_ADD_PIXEL_LABEL = "add pixel to #{id}, color: {color}, position: {position}";

    private PixelArt mPixelArt = null;
    private PixelArtView mView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mView = new PixelArtView(this);
        mView.setOnSelectPixelsListener(this);

        setContentView(mView);

        Bundle extra = getIntent().getExtras();
        if (extra != null && mPixelArt == null) {
            if (extra.containsKey("id") && extra.getInt("id") > 0) {
                mPixelArt = new PixelArt();
                mPixelArt.id = extra.getInt("id");
                mPixelArt.locked = true;

                PixelArt pixelArt = PixelArtStorage.loadPixelArt(this, mPixelArt.id);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                }

                if (extra.containsKey("title")) {
                    mPixelArt.title = extra.getString("title");
                }
                if (extra.containsKey("width")) {
                    mPixelArt.width = extra.getInt("width");
                }
                if (extra.containsKey("height")) {
                    mPixelArt.height = extra.getInt("height");
                }

                loadPixelArt(mPixelArt.id);
                getActionBar().setTitle(mPixelArt.title);
            }
        }


        Tracker t = ((CPixelArtsApplication) getApplication()).getTracker();

        if (mPixelArt == null)  {
            mPixelArt = new PixelArt();
            mPixelArt.title = getString(R.string.new_pixel_art);
            mPixelArt.width = 16;
            mPixelArt.height = 16;
            mPixelArt.pixels = new int[256];
            Arrays.fill(mPixelArt.pixels, -1);

            t.setScreenName("New Pixel Art");
        } else {
            t.setScreenName("Edit Pixel Art: " + mPixelArt.id);
        }

        t.send(new HitBuilders.AppViewBuilder().build());

        displayPixelArt();
    }

    protected void displayPixelArt()
    {
        if (mPixelArt == null) {
            return;
        }

        if (mPixelArt.title != null) {
            getActionBar().setTitle(mPixelArt.title);
        } else if (mPixelArt.id > 0) {
            getActionBar().setTitle("#" + mPixelArt.id);
        }
        mView.setPixelArt(mPixelArt);
        mView.invalidate();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mPixelArt.id < 0 || mPixelArt.title != null) {
            menu.removeItem(R.id.action_name_it);
        }
        if (mPixelArt.id < 0 || mPixelArt.locked) {
            menu.removeItem(R.id.action_lock_it);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pixel_art, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_name_it) {
            nameItAction();
            return true;
        } else if (id == R.id.action_lock_it) {
            lockItAction();
            return true;
        } else if (id == R.id.action_share) {
            shareItAction();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void nameItAction()
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Name this Pixel Art");

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String url = SET_TITLE_URL.replace("{id}", Integer.toString(mPixelArt.id));
                String title = input.getText().toString();
                if (!title.isEmpty()) {
                    List<NameValuePair> patchParams = new ArrayList<NameValuePair>();
                    patchParams.add(new BasicNameValuePair("title", title));

                    new SetTileTask(patchParams).execute(url);
                }
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    protected void lockItAction() {
        String url = LOCK_URL.replace("{id}", Integer.toString(mPixelArt.id));
        new LockPixelArtTask().execute(url);
    }

    // Call to update the share intent
    private void shareItAction() {
        String title = mPixelArt.title != null ? mPixelArt.title : ("#" + Integer.toString(mPixelArt.id));
        String titleCanonical = mPixelArt.titleCanonical != null ? mPixelArt.titleCanonical : Integer.toString(mPixelArt.id);
        String url = GalleryActivity.BASE_URL + VIEW_PIXEL_ART_URL.replace("{title}", titleCanonical);
        String imageUrl = GalleryActivity.BASE_URL + PIXEL_ART_FILE_URL.replace("{title}", titleCanonical);
        Uri imageUri = Uri.parse(imageUrl);
        String appName = getString(R.string.app_name);

        List<Intent> targetedShareIntents = new ArrayList<Intent>();
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        List<ResolveInfo> resInfo = getPackageManager().queryIntentActivities(shareIntent, 0);
        if (!resInfo.isEmpty()) {
            for (ResolveInfo resolveInfo : resInfo) {
                String packageName = resolveInfo.activityInfo.packageName;
                Intent targetedShareIntent = new Intent(android.content.Intent.ACTION_SEND);
                targetedShareIntent.setType("text/plain");
                targetedShareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title + " - " + appName);

                if (packageName.equals("com.facebook.katana")) {
                    targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
                } else if (packageName.equals("com.twitter.android")) {
                    targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, title + " - " + url + " #PixelArts via @cPixelArts");
                } else if (packageName.equals("com.google.android.apps.plus")) {
                    targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, title + " - " + url);
                } else {
                    targetedShareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                    targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT, title + " - " + appName + " - " + url);
                }

                targetedShareIntent.setPackage(packageName);
                targetedShareIntents.add(targetedShareIntent);
            }
            Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), "Select app to share");

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));

            startActivity(chooserIntent);
        }
    }

    // Uses AsyncTask subclass to download the pixel arts from api
    private void loadPixelArt(int id) {
        String url = PIXEL_ART_URL.replace("{id}", Integer.toString(id));
        new DownloadPixelArtTask().execute(url);
    }

    @Override
    public void onSelectPixels(Integer[] position) {

        List<NameValuePair> postParams = new ArrayList<NameValuePair>();
        postParams.add(new BasicNameValuePair("color", Integer.toString(mView.getCurrentColor())));
        int i = 0;
        for (Integer pos : position) {
            postParams.add(new BasicNameValuePair("position[" + (i++) + "]", pos.toString()));
        }

        String url;
        String label = EVENT_ADD_PIXEL_LABEL
                .replace("{color}", Integer.toString(mView.getCurrentColor()))
                .replace("{position}", Arrays.toString(position));
        if (mPixelArt.id < 0) {
            url = CREATE_PIXEL_ART_URL;
            label = label.replace("{id}", "new pixel art");
        } else {
            url = ADD_PIXEL_URL.replace("{id}", Integer.toString(mPixelArt.id));
            label = label.replace("{id}", Integer.toString(mPixelArt.id));
        }

        new AddPixelTask(postParams).execute(url);

        Tracker t = ((CPixelArtsApplication) getApplication()).getTracker();

        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(EVENT_ADD_PIXEL_CATEGORY)
                .setAction(EVENT_ADD_PIXEL_ACTION)
                .setLabel(label)
                .build());
    }

    private class DownloadPixelArtTask extends GetAsyncTask {
        private static final String errorMsg = "Unable to retrieve pixel art.";
        private ProgressDialog mDialog = null;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(PixelArtActivity.this);
            mDialog.setMessage(getString(R.string.msg_loading));
            mDialog.show();
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            mDialog.dismiss();

            if (result != null) {
                mPixelArt = PixelArtParser.parsePixelArt(result);
                PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);
                displayPixelArt();
            } else {
                mPixelArt.locked = true;
                Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class AddPixelTask extends PostAsyncTask {
        private ProgressDialog mDialog = null;

        public AddPixelTask(List<NameValuePair> postParams) {
            super(postParams);
        }

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(PixelArtActivity.this);
            mDialog.setMessage(getString(R.string.msg_adding_pixel));
            mDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mDialog.dismiss();

            if (result == null) {
                mPixelArt.locked = true;
                Toast.makeText(getBaseContext(), R.string.msg_unable_add_pixel, Toast.LENGTH_SHORT).show();
                displayPixelArt();
                return;
            }

            PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
            if (pixelArt != null) {
                mPixelArt = pixelArt;
                PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);

                if (getIntent().hasExtra("new")) {
                    getIntent().removeExtra("new");
                    getIntent().putExtra("id", mPixelArt.id);
                }

                if (mView != null) {
                    mView.setPixelArt(mPixelArt);
                }
                invalidateOptionsMenu();
            }
        }
    }

    private class SetTileTask extends PatchAsyncTask {
        public SetTileTask(List<NameValuePair> postParams) {
            super(postParams);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                    PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);
                }
            } else {
                Toast.makeText(getBaseContext(), R.string.msg_unable_name, Toast.LENGTH_SHORT).show();
            }
            displayPixelArt();
        }
    }

    private class LockPixelArtTask extends PatchAsyncTask {
        public LockPixelArtTask() {
            super(null);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                    PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);
                }
            } else {
                Toast.makeText(getBaseContext(), R.string.msg_unable_lock, Toast.LENGTH_SHORT).show();
            }
            displayPixelArt();
        }
    }
}
