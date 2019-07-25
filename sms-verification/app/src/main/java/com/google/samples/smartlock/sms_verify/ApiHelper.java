package com.google.samples.smartlock.sms_verify;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by pmatthews on 9/16/16.
 */

public class ApiHelper extends ContextWrapper {
    public static final int VERSION_GMS_V8_MAX = 10200000;
    private static final String TAG = ApiHelper.class.getSimpleName();

    public static final String RESPONSE_SUCCESS = "success";
    public static final String RESPONSE_PHONE = "phone";

    private RequestQueue requestQueue;
    private PrefsHelper prefsHelper;

    public ApiHelper(Context base) {
        super(base);
        requestQueue = Volley.newRequestQueue(this);
        prefsHelper = new PrefsHelper(this);
    }

    public void request(String phoneNo, final RequestResponse successReceiver,
                        final ApiError failureReceiver) {
        HashMap<String, String> params = new HashMap<>();
        params.put("phone", phoneNo);
        sendRequest(R.string.url_request, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Boolean success = false;
                try {
                    success = response.getBoolean(RESPONSE_SUCCESS);
                } catch (JSONException e) {
                    Log.e(TAG, "Possible missing response value.", e);
                }
                successReceiver.onResponse(success);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                failureReceiver.onError(error);
            }
        });
    }

    public void verify(String phoneNo, String smsMessage, final VerifyResponse successReceiver,
                       final ApiError failureReceiver) {
        HashMap<String, String> params = new HashMap<>();
        params.put("phone", phoneNo);
        params.put("sms_message", smsMessage);

        sendRequest(R.string.url_verify, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Boolean success = false;
                String phoneNo = null;
                try {
                    success = response.getBoolean(RESPONSE_SUCCESS);
                    phoneNo = response.getString(RESPONSE_PHONE);
                } catch (JSONException e) {
                    Log.e(TAG, "Possible missing response value.", e);
                }
                successReceiver.onResponse(success, phoneNo);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                failureReceiver.onError(error);
            }
        });
    }

    public void reset(String phoneNo, final ResetResponse successReceiver,
                      final ApiError failureReceiver) {
        HashMap<String, String> params = new HashMap<>();
        params.put("phone", phoneNo);
        sendRequest(R.string.url_reset, params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Boolean success = false;
                try {
                    success = response.getBoolean(RESPONSE_SUCCESS);
                } catch (JSONException e) {
                    Log.e(TAG, "Possible missing response value.", e);
                }
                successReceiver.onResponse(success);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                failureReceiver.onError(error);
            }
        });
    }

    protected void sendRequest(int urlId, HashMap<String, String> params, Response.Listener success,
                            Response.ErrorListener failure) {
        sendRequest(getString(urlId), params, success, failure);
    }

    protected void sendRequest(String url, HashMap<String, String> params, Response.Listener success,
                             Response.ErrorListener failure) {
        String secret = prefsHelper.getSecretOverride(null);
        if (TextUtils.isEmpty(secret)) {
            final int gmsVersion = getGmsVersion(getApplicationContext());
            Log.d(TAG, "GMS Version: " + gmsVersion);
            secret = getString(R.string.server_client_secret);
            if (gmsVersion  < VERSION_GMS_V8_MAX) {
                secret = getString(R.string.server_client_secret_v8);
            }
        }
        try {
            JSONObject args = new JSONObject();
            args.put("client_secret", secret);
            for (String key: params.keySet()) {
                args.put(key, params.get(key));
            }
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url,
                    args, success, failure);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to make the request", e);
        }
    }

    private static int getGmsVersion(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo("com.google.android.gms", 0 ).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return VERSION_GMS_V8_MAX;
    }

    public interface RequestResponse {
        public void onResponse(boolean success);
    }

    public interface VerifyResponse {
        public void onResponse(boolean success, String phoneNumber);
    }

    public interface ResetResponse {
        public void onResponse(boolean success);
    }

    public interface ApiError {
        public void onError(VolleyError error);
    }
}
