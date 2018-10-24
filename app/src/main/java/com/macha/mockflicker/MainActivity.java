package com.macha.mockflicker;

import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private GenericUtil genericUtil = new GenericUtil();
    private Button loginButton;
    private ProgressBar spinner;
    private ConstraintLayout constraintLayout;
    private String authSecret = "";
    private String finalAuthToken = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        constraintLayout = (ConstraintLayout) findViewById(R.id.mainLayoutId);

        spinner = (ProgressBar) findViewById(R.id.loginProgressBarId);
        loginButton = (Button) findViewById(R.id.loginButton);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startActivity(new Intent(MainActivity.this, OauthActivity.class));
                loginButton.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                try {
                    getRequestToken();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void getRequestToken() throws UnsupportedEncodingException, InvalidKeyException, NoSuchAlgorithmException {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // These params should ordered in key
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("oauth_callback", "oauth://oauthresponse"));
        qparams.add(new BasicNameValuePair("oauth_consumer_key", Constants.apiKey));
        qparams.add(new BasicNameValuePair("oauth_nonce", "" + (int) (Math.random() * 100000000)));
        qparams.add(new BasicNameValuePair("oauth_signature_method", "HMAC-SHA1"));
        qparams.add(new BasicNameValuePair("oauth_timestamp", "" + (System.currentTimeMillis())));
        qparams.add(new BasicNameValuePair("oauth_version", "1.0"));

        // generate the oauth_signature
        String signature = genericUtil.getSignature(Constants.apiSecret, URLEncoder.encode(
                "http://www.flickr.com/services/oauth/request_token", "UTF-8"),
                URLEncoder.encode(URLEncodedUtils.format(qparams, "UTF-8"), "UTF-8"),
                null);

        // add it to params list
        qparams.add(new BasicNameValuePair("oauth_signature", signature));

        // generate URI which lead to access_token and token_secret.
        String url = "https://www.flickr.com/services/oauth/request_token?" + URLEncodedUtils.format(qparams, "UTF-8");
        System.out.println("Get Token and Token Secrect from:" + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        System.out.println("Response is: " + response.substring(0, 500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String body = "";
                String authToken = "";
                try {
                    body = new String(error.networkResponse.data, "UTF-8");
                    Map<String, String> paramMap = genericUtil.createMapFromResponse(body);
                    authToken = paramMap.get("oauth_token");
                    authSecret = paramMap.get("oauth_token_secret");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(error.getMessage());

                authorize(authToken);
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void authorize(String token) {
        final WebView web = new WebView(this);

        web.getSettings().setJavaScriptEnabled(true);
        web.loadUrl("https://www.flickr.com/services/oauth/authorize?oauth_token=" + token);

        constraintLayout.addView(web);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String token = intent.getData().getQueryParameter("oauth_token");
        String verifier = intent.getData().getQueryParameter("oauth_verifier");

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // These params should ordered in key
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("oauth_consumer_key", Constants.apiKey));
        qparams.add(new BasicNameValuePair("oauth_nonce", "" + (int) (Math.random() * 100000000)));
        qparams.add(new BasicNameValuePair("oauth_signature_method", "HMAC-SHA1"));
        qparams.add(new BasicNameValuePair("oauth_timestamp", "" + (System.currentTimeMillis())));
        qparams.add(new BasicNameValuePair("oauth_token", token));
        qparams.add(new BasicNameValuePair("oauth_verifier", verifier));
        qparams.add(new BasicNameValuePair("oauth_version", "1.0"));

        // generate the oauth_signature
        String signature = null;
        try {
            signature = genericUtil.getSignature(Constants.apiSecret, URLEncoder.encode(
                    "https://www.flickr.com/services/oauth/access_token", "UTF-8"),
                    URLEncoder.encode(URLEncodedUtils.format(qparams, "UTF-8"), "UTF-8"),
                    authSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // add it to params list
        qparams.add(new BasicNameValuePair("oauth_signature", signature));

        // generate URI which lead to access_token and token_secret.
        String url = "https://www.flickr.com/services/oauth/access_token?" + URLEncodedUtils.format(qparams, "UTF-8");
        System.out.println("Get Token and Token Secrect from:" + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        System.out.println("Response is: " + response.toString());
                        Map<String, String> paramMap = genericUtil.createMapFromResponse(response.toString());
                        finalAuthToken = paramMap.get("oauth_token");
                        authSecret = paramMap.get("oauth_token_secret");
                        makeUserTestCall(finalAuthToken);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                System.out.println(error.getMessage());
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void makeUserTestCall(String token) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        // These params should ordered in key
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();

        qparams.add(new BasicNameValuePair("format", "json"));
        qparams.add(new BasicNameValuePair("method", "flickr.test.login"));
        qparams.add(new BasicNameValuePair("nojsoncallback", "1"));
        qparams.add(new BasicNameValuePair("oauth_consumer_key", Constants.apiKey));
        qparams.add(new BasicNameValuePair("oauth_nonce", "" + (int) (Math.random() * 100000000)));
        qparams.add(new BasicNameValuePair("oauth_signature_method", "HMAC-SHA1"));
        qparams.add(new BasicNameValuePair("oauth_timestamp", "" + (System.currentTimeMillis())));
        qparams.add(new BasicNameValuePair("oauth_token", token));
        qparams.add(new BasicNameValuePair("oauth_version", "1.0"));

        // generate the oauth_signature
        String signature = null;
        try {
            signature = genericUtil.getSignature(Constants.apiSecret, URLEncoder.encode(
                    "https://api.flickr.com/services/rest", "UTF-8"),
                    URLEncoder.encode(URLEncodedUtils.format(qparams, "UTF-8"), "UTF-8"),
                    authSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // add it to params list
        qparams.add(new BasicNameValuePair("oauth_signature", signature));

        // generate URI which lead to access_token and token_secret.
        String url = "https://api.flickr.com/services/rest?" + URLEncodedUtils.format(qparams, "UTF-8");
        System.out.println("Auth test:" + url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        System.out.println("Response is: " + response.toString());

                        makeGetRecentPhotosCall();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String body = "";
                try {
                    body = new String(error.networkResponse.data,"UTF-8");
                    System.out.println(body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(error.getMessage());
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }


    private void makeGetRecentPhotosCall() {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        StringBuilder url = new StringBuilder(Constants.flickApi);
        url.append("flickr.photos.getRecent");
        url.append("&api_key=" + Constants.apiKey);
        url.append("&" + Constants.restFormat);
        url.append("&nojsoncallback=1");

        JSONObject jsonObject = new JSONObject();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url.toString(), jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject responseJsonObject) {
                        System.out.println("Response is: " + responseJsonObject.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String body = "";
                try {
                    body = new String(error.networkResponse.data,"UTF-8");
                    System.out.println(body);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(error.getMessage());
            }
        });

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }
}

