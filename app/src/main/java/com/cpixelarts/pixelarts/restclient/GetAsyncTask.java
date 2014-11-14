package com.cpixelarts.pixelarts.restclient;

import android.os.AsyncTask;

import com.cpixelarts.pixelarts.GalleryActivity;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by vincent on 14/11/14.
 */
public abstract class GetAsyncTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response;
        try {
            String url = GalleryActivity.BASE_URL + params[0];

            HttpGet httpget = new HttpGet(url);
            response = httpclient.execute(httpget);
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
            //e.printStackTrace();
        } catch (IOException e) {
            //TODO Handle problems..
            //e.printStackTrace();
        }

        return null;
    }

}
