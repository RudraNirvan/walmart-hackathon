package com.rudra.walmartnotifier;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.chrome.ChromeDriver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Rudra on 05-06-2015.
 */
public class WalmartService extends Service {
    WebView webView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "You can now close the app.", Toast.LENGTH_LONG).show();
        webView = new WebView(this);
        new PullData().execute();

        stopSelf();
        return 1;
    }

    public class PullData extends AsyncTask<Void, Void, String> {
        PullData() {
        }

        @Override
        protected String doInBackground(Void... params) {
            String apiKey = "yxb4f8t6cux92g2u2x8ezprh";
            String userLat = "40.7337697";
            String userLon = "-73.7988285";

            ArrayList<String> storeLat = new ArrayList<>(), storeLon = new ArrayList<>(),
                    storeNo = new ArrayList<>(); // Calculated in Step 1
            String selectedStoreNo = ""; // Calculated in step 3
            int selectedLatLonIndex = 0; // Calculated in Step 4

            ArrayList<String> itemId = new ArrayList<>(), itemName = new ArrayList<>(), itemSalePrice = new ArrayList<>(),
                    addToCartURL = new ArrayList<>(), thumbnailURL = new ArrayList<>(); // calculated in Step 2
            int selectedItemIndex = 0; // Calculated in step 3


            Bitmap thumbnail = null; // Calculated in Step 5

            String ret = "";

            /*
                STEP 1: Search for a nearby store
             */
            try {
                String urlString = "http://api.walmartlabs.com/v1/stores?apiKey=" + apiKey +
                        "&lat=" + userLat + "&lon=" + userLon + "&format=json";

                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(20 * 1000);
                con.setConnectTimeout(20 * 1000);
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.connect();

                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String line;
                while ((line = br.readLine()) != null) {
                    ret += line;
                }

                con.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String combined = "";

            if (ret != "") {
                try {
                    JSONArray places = new JSONArray(ret);
                    for (int i = 0; i < places.length(); i++) {
                        JSONObject object = places.getJSONObject(i);

                        storeNo.add(object.getString("no"));
                        combined = object.getString("coordinates");

                        String temp[] = combined.split(",");
                        storeLon.add(temp[0].trim());
                        storeLat.add(temp[1].trim());
                        storeLon.set(i, storeLon.get(i).substring(1, storeLon.get(i).length()));
                        storeLat.set(i, storeLat.get(i).substring(0, storeLat.get(i).length() - 1));


                        Log.d("rp", storeLat.get(i) + " & " + storeLon.get(i) + " = " + storeNo.get(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            /*
                STEP 2: Recommend products based on customer's purchase history
             */
            try {
                ret = "";
                String purchasedItemId = "38456901"; // user's item on which recommendations are to be made
                String urlString = "http://api.walmartlabs.com/v1/nbp?apiKey=" + apiKey +
                        "&itemId=" + purchasedItemId + "&format=json";

                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(20 * 1000);
                con.setConnectTimeout(20 * 1000);
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.connect();

                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String line;
                while ((line = br.readLine()) != null) {
                    ret += line;
                }

                con.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (ret != "") {
                try {
                    // [NOT ANYMORE] take the first item to display

                    JSONArray items = new JSONArray(ret);

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject object = items.getJSONObject(i);

                        itemId.add(object.getString("itemId"));
                        itemName.add(object.getString("name"));
                        itemSalePrice.add(object.getString("salePrice"));
                        addToCartURL.add(object.getString("addToCartUrl"));
                        thumbnailURL.add(object.getString("thumbnailImage"));

                        Log.d("rp", itemId.get(i) + ": " + itemName.get(i) + " @ " + itemSalePrice);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


            /*
                STEP 3: Calculate stores' numbers where the recommended products are in stock
                        First, choose the first product which is available,
                        then, choose the first nearby store where it is available
             */

            for (int i = 0; i < itemId.size(); i++) {
                boolean found = false;
                final String URLString = "http://mobile.walmart.com/find-in-store/" + itemId.get(i) + "/" + userLat + "/" + userLon;
                final StringBuffer sb = new StringBuffer(); // http://stackoverflow.com/questions/1249917/final-variable-manipulation-in-java

//                Log.d("checkkk", "-1 here 1");
                final CountDownLatch latch = new CountDownLatch(1);

                class MyJavaScriptInterface {
                    @JavascriptInterface
                    public void processHTML(String html) {
                        sb.append(html);
                        latch.countDown();
                        Log.d("checkkk", sb.toString().substring(0, 88));
                    }
                }
                Log.d("checkkk", "-2 here 2");

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
//                        Log.d("checkkk", "runnable begins");
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public void onPageFinished(WebView view, String url) {
                                // Need to inserr delay as the Walmart site JS takes some time to load
                                Log.d("checkkk", "go to sleep :)");
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d("checkkk", "awake");
                                        webView.loadUrl("javascript:window.HTMLOUT.processHTML('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
                                        Log.d("checkkk", "loaded.....................");
                                    }
                                }, 5 * 1000);
                            }
                        });
                        webView.loadUrl(URLString);
                        Log.d("checkkk", "runnable ends");
                    }
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Document doc = Jsoup.parse(sb.toString());
                Elements elements = doc.select(".location-list-item"); // these are the stores
                Log.d("checkkk", "4 = " + elements.size());
                for (Element e : elements) {
                    String _text = e.select("p").first().text();
                    if (_text.equals("In stock")) {
                        String _storeNo = e.attr("data-model-id");
                        selectedStoreNo = _storeNo;
                        selectedItemIndex = i;
                        found = true;
                        break;
                    }
                }

                if (found) {
                    Log.d("checkkk", "FOUNDDDDDDDDDDDD");
                    break;
                }
            }

            /*
                STEP 4: Get the coordinates of selected store
             */

            for (int i = 0; i < storeNo.size(); i++) {
                if (storeNo.get(i).equals(selectedStoreNo)) {
                    selectedLatLonIndex = i;
                }
            }


            /*
                STEP 5: Show notification; two options - add to cart, navigate to store; also add thumbnail
             */

            // Load thumbnail image for notification
            try {
                URL url = new URL(thumbnailURL.get(selectedItemIndex));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setDoInput(true);
                con.connect();

                InputStream is = con.getInputStream();
                thumbnail = BitmapFactory.decodeStream(is);

                con.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            String navURL = "http://maps.google.com/maps?saddr=" + userLat + "," + userLon + "&daddr=" + storeLat.get(selectedLatLonIndex) +
                    "," + storeLon.get(selectedLatLonIndex);
            Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse(navURL));
            in.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
            PendingIntent mapsIntent = PendingIntent.getActivity(getApplicationContext(), 0, in, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(addToCartURL.get(selectedItemIndex)));
            PendingIntent cartIntent = PendingIntent.getActivity(getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            @SuppressLint({"NewApi", "LocalSuppress"}) Notification notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle("SALE: Just for $" + itemSalePrice.get(selectedItemIndex))
                    .setContentText(itemName.get(selectedItemIndex))
                    .setSmallIcon(R.drawable.walmart_logo)
                    .setLargeIcon(thumbnail)
                    .setAutoCancel(true)
                    .setStyle(new Notification.BigTextStyle().bigText(itemName.get(selectedItemIndex)))
                    .addAction(R.drawable.ic_cart, "Cart", cartIntent)
                    .addAction(R.drawable.ic_navigation, "Navigate", mapsIntent)
                    .getNotification();
            notification.defaults |= Notification.DEFAULT_SOUND;
            notificationManager.notify(25, notification);

            return ret;
        }

        @Override
        protected void onPostExecute(String json) {
//            Log.d("rp", json);
        }
    }

}
