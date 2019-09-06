package com.google.samples.smartlock.sms_verify.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.samples.smartlock.sms_verify.ApiHelper;
import com.google.samples.smartlock.sms_verify.PhoneNumberVerifier;
import com.google.samples.smartlock.sms_verify.PrefsHelper;
import com.google.samples.smartlock.sms_verify.R;

import org.json.JSONException;
import org.json.JSONObject;

import static com.google.samples.smartlock.sms_verify.ApiHelper.RESPONSE_SUCCESS;

public class ResetActivity extends PhoneNumberActivity {

    public static final String TAG = ResetActivity.class.getSimpleName();

    private ApiHelper api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = new ApiHelper(this);
    }

    @Override
    protected String getActivityTitle() {
        return getString(R.string.reset_title);
    }

    @Override
    protected void doSubmit(final String phoneNumber) {
        final PrefsHelper prefs = getPrefs();
        PhoneNumberVerifier.stopActionVerify(this);
        if (!phoneNumber.isEmpty()) {
            ui.setSubmitEnabled(false);
            api.reset(phoneNumber,
                    new ApiHelper.ResetResponse() {
                        @Override
                        public void onResponse(boolean success) {
                            if (!success) {
                                Log.d(TAG, "Response was failure.");
                                Toast.makeText(
                                        ResetActivity.this,
                                        getString(R.string.toast_unable_to_reset),
                                        Toast.LENGTH_LONG).show();
                            }

                            String storedPhone = prefs.getPhoneNumber(null);
                            if (phoneNumber.equals(storedPhone)) {
                                prefs.removeVerified();
                                prefs.removePhoneNumber();
                            }
                            finish();
                        }
                    }, new ApiHelper.ApiError() {
                        @Override
                        public void onError(VolleyError error) {
                            ui.setSubmitEnabled(true);
                            // Do something else.
                            Log.d(TAG, "Error getting response");
                            Toast.makeText(ResetActivity.this, getString(R.string.toast_something_wrong),
                                    Toast.LENGTH_LONG).show();
                            finish();
                        }
                    });
        }
    }
}
