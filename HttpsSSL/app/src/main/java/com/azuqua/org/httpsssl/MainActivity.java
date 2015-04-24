package com.azuqua.org.httpsssl;

import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.text.SimpleDateFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends ActionBarActivity {
    HttpClient httpclient;
    String accessKey = "4406195d59eab7d86d2fa88cfebc432c32c80ce8";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        httpclient = getNewHttpClient();

        new GetFlos().execute();
    }



    public HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore
                    .getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory
                    .getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(
                    params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    private class GetFlos extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {

            String URL = "http://api.azuqua.com/account/flos";
            
            String timestamp = getISOTime();
            
            String hash = signData(timestamp);
            
            HttpGet httpget = new HttpGet(URL);
            httpget.addHeader("Content-Type", "application/json; charset=utf-8");
            httpget.addHeader("Content-Length", "0");
            httpget.addHeader("x-api-timestamp", timestamp);
            httpget.addHeader("x-api-hash", hash);
            httpget.addHeader("x-api-accessKey", accessKey);

            HttpResponse response = null;
            try {
                response = httpclient.execute(httpget);

            } catch (IOException e) {
                e.printStackTrace();
            }
            HttpEntity entity = response.getEntity();
            try {
                Log.i("SASI API", EntityUtils.toString(entity));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private String signData(String timestamp) {
        String accessSecret = "998f85a5b56f089a86b16335c42b7ea604948c29bc5619085be2ed172daeb12e";

        String verb = "get";
        String path ="/account/flos";
        String data = "";

        Mac hmac = null;
        try {
            hmac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        SecretKeySpec key = null;
        try {
            key = new SecretKeySpec(accessSecret.getBytes("UTF-8"), "HmacSHA256");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            hmac.init(key);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        String meta = verb + ":" + path + ":" + timestamp;
        String dataToDigest = meta + data;


        byte[] digest = new byte[0];
        try {
            digest = hmac.doFinal(dataToDigest.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String digestString = bytesToHex(digest).toLowerCase();

        return 	digestString;

    }

    private String getISOTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String timestamp = df.format(new Date());

        return timestamp;

    }

    private String bytesToHex(byte[] bytes) {
        final char hexArray[] = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
