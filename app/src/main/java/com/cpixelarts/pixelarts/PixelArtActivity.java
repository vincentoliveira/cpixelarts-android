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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.cpixelarts.pixelarts.model.PixelArt;
import com.cpixelarts.pixelarts.storage.PixelArtStorage;
import com.cpixelarts.pixelarts.utils.PixelArtParser;
import com.cpixelarts.pixelarts.view.PixelArtView;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class PixelArtActivity extends Activity implements PixelArtView.OnSelectColorListener, PixelArtView.OnSelectPixelListener {
    private static final String VIEW_PIXEL_ART_URL = "/drawing/{title}";
    private static final String PIXEL_ART_FILE_URL = "/gallery/drawing-{title}_512.png";
    private static final String PIXEL_ART_URL = "/api/drawings/{id}";
    private static final String CREATE_PIXEL_ART_URL = "/api/drawings";
    private static final String ADD_PIXEL_URL = "/api/drawings/{id}/pixels";
    private static final String SET_TITLE_URL = "/api/drawings/{id}/title";
    private static final String LOCK_URL = "/api/drawings/{id}/lock";

    private PixelArt mPixelArt = null;
    private PixelArtView mView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mView = new PixelArtView(this);
        mView.setOnSelectColorListener(this);
        mView.setOnSelectPixelListener(this);

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

        if (mPixelArt == null)  {
            mPixelArt = new PixelArt();
            mPixelArt.title = getString(R.string.new_pixel_art);
            mPixelArt.width = 16;
            mPixelArt.height = 16;
            mPixelArt.pixels = new int[256];
            Arrays.fill(mPixelArt.pixels, -1);
        }

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
                    new SetTileTask().execute(url, title);
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
    public void onSelectColor(int color) {
        // Do nothing
    }

    @Override
    public void onSelectPixel(int position) {
        mPixelArt.pixels[position] = mView.getCurrentColor();
        if (mPixelArt.id < 0) {
            createPixelArt(mView.getCurrentColor(), position);
        } else {
            addPixelToPixelArt(mView.getCurrentColor(), position);
        }
    }

    protected void createPixelArt(int color, int position) {
        String url = CREATE_PIXEL_ART_URL;
        new CreatePixelArtTask().execute(url, Integer.toString(color), Integer.toString(position));
    }

    protected void addPixelToPixelArt(int color, int position) {
        if (mPixelArt.id < 0) {
            return;
        }
        String url = ADD_PIXEL_URL.replace("{id}", Integer.toString(mPixelArt.id));
        new AddPixelTask().execute(url, Integer.toString(color), Integer.toString(position));
    }

    private class DownloadPixelArtTask extends AsyncTask<String, Void, String> {
        private static final String errorMsg = "Unable to retrieve pixel art.";
        private ProgressDialog mDialog = null;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(PixelArtActivity.this);
            mDialog.setMessage(getString(R.string.msg_loading));
            mDialog.show();
        }

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
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            mDialog.dismiss();

            if (!result.equals(errorMsg)) {
                mPixelArt = PixelArtParser.parsePixelArt(result);
                PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);
                displayPixelArt();
            } else {
                mPixelArt.locked = true;
                Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class CreatePixelArtTask extends AsyncTask<String, Void, String> {
        private static final String errorMsg = "Unable to create pixel art.";
        private ProgressDialog mDialog = null;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(PixelArtActivity.this);
            mDialog.setMessage(getString(R.string.msg_creating));
            mDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                String url = GalleryActivity.BASE_URL + params[0];
                List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                postParams.add(new BasicNameValuePair("color", params[1]));
                postParams.add(new BasicNameValuePair("position", params[2]));

                HttpPost httppost = new HttpPost(url);
                httppost.setEntity(new UrlEncodedFormEntity(postParams));
                response = httpclient.execute(httppost);
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
            mDialog.dismiss();

            if (!result.equals(errorMsg)) {
                PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                    PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);

                    getIntent().removeExtra("new");
                    getIntent().putExtra("id", mPixelArt.id);

                    if (mView != null) {
                        mView.setPixelArt(mPixelArt);
                    }
                    invalidateOptionsMenu();
                }
            } else {
                mPixelArt.locked = true;
                Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_SHORT).show();
                displayPixelArt();
            }
        }
    }

    private class AddPixelTask extends AsyncTask<String, Void, String> {
        private static final String errorMsg = "Unable to add pixel.";
        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                String url = GalleryActivity.BASE_URL + params[0];
                List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                postParams.add(new BasicNameValuePair("color", params[1]));
                postParams.add(new BasicNameValuePair("position", params[2]));

                HttpPost httppost = new HttpPost(url);
                httppost.setEntity(new UrlEncodedFormEntity(postParams));
                response = httpclient.execute(httppost);
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
            if (!result.equals(errorMsg)) {
                PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                    PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);

                    if (mView != null) {
                        mView.setPixelArt(mPixelArt);
                    }
                }
            } else {
                Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_SHORT).show();
                displayPixelArt();
            }
        }
    }

    private class SetTileTask extends AsyncTask<String, Void, String> {
        private static final String errorMsg = "Unable to name this Pixel Art.";
        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                String url = GalleryActivity.BASE_URL + params[0];
                List<NameValuePair> postParams = new ArrayList<NameValuePair>();
                postParams.add(new BasicNameValuePair("title", params[1]));

                HttpPatch httppatch = new HttpPatch(url);
                httppatch.setEntity(new UrlEncodedFormEntity(postParams));
                response = httpclient.execute(httppatch);
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
            if (!result.equals(errorMsg)) {
                PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                    PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);
                }
            } else {
                Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
            displayPixelArt();
        }
    }

    private class LockPixelArtTask extends AsyncTask<String, Void, String> {
        private static final String errorMsg = "Unable to lock this Pixel Art.";
        @Override
        protected String doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            try {
                String url = GalleryActivity.BASE_URL + params[0];
                response = httpclient.execute(new HttpPatch(url));
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
            if (!result.equals(errorMsg)) {
                PixelArt pixelArt = PixelArtParser.parsePixelArt(result);
                if (pixelArt != null) {
                    mPixelArt = pixelArt;
                    PixelArtStorage.storePixelArt(getBaseContext(), mPixelArt.id, result);
                }
            } else {
                Toast.makeText(getBaseContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
            displayPixelArt();
        }
    }
}
