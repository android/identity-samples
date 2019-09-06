package com.google.samples.smartlock.sms_verify.ui;

import android.util.Log;

import com.google.samples.smartlock.sms_verify.PhoneNumberVerifier;
import com.google.samples.smartlock.sms_verify.R;

public class SignUpActivity extends PhoneNumberActivity {
    public static final String TAG = SignUpActivity.class.getSimpleName();

    @Override
    protected String getActivityTitle() {
        return getString(R.string.sign_up_title);
    }

    @Override
    protected void doSubmit(String phoneValue) {
        Log.d(TAG, "Using the phone number.");
        PhoneNumberVerifier.startActionVerify(this, phoneValue);
        finish();
    }
}
