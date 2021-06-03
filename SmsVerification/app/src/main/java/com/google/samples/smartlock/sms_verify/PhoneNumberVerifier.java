package com.google.samples.smartlock.sms_verify;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * A service to verify a telephone number.
 */
public class PhoneNumberVerifier extends Service {
    private static final String TAG = PhoneNumberVerifier.class.getSimpleName();
    public static final String ACTION_VERIFICATION_STATUS_CHANGE = "com.google.samples.smartlock.sms_verify.action.verification_status_change";

    public static final int STATUS_STARTED = 10;
    public static final int STATUS_REQUESTING = 20;
    public static final int STATUS_REQUEST_SENT = 30;
    public static final int STATUS_RESPONSE_RECEIVED = 50;
    public static final int STATUS_RESPONSE_VERIFYING = 70;
    public static final int STATUS_RESPONSE_VERIFIED = 90;
    public static final int STATUS_RESPONSE_FAILED = 95;
    public static final int STATUS_MAX = 100;

    public static final int MAX_TIMEOUT = 1800000; // 30 mins in millis

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_VERIFY = "com.google.samples.smartlock.sms_verify.action.verify";
    private static final String ACTION_VERIFY_MESSAGE = "com.google.samples.smartlock.sms_verify.action.message";
    private static final String ACTION_CANCEL = "com.google.samples.smartlock.sms_verify.action.cancel";
    private static final String EXTRA_PHONE_NUMBER = "com.google.samples.smartlock.sms_verify.extra.PHONE_NUMBER";
    private static final String EXTRA_MESSAGE = "com.google.samples.smartlock.sms_verify.extra.MESSAGE";

    private static final int NOTIFICATION_ID = 1001;

    private static boolean isVerifying = false;

    private SmsRetrieverClient smsRetrieverClient;
    private NotificationCompat.Builder notification;
    private PrefsHelper prefs;
    private ApiHelper api;
    private SmsBrReceiver smsReceiver;

    private static boolean isRunning;

    public static void startActionVerify(Context context, String phoneNo) {
        Intent intent = new Intent(context, PhoneNumberVerifier.class);
        intent.setAction(ACTION_VERIFY);
        intent.putExtra(EXTRA_PHONE_NUMBER, phoneNo);
        context.startService(intent);
    }

    public static void startActionVerifyMessage(Context context, String message) {
        Intent intent = new Intent(context, PhoneNumberVerifier.class);
        intent.setAction(ACTION_VERIFY_MESSAGE);
        intent.putExtra(EXTRA_MESSAGE, message);
        context.startService(intent);
    }

    public static void stopActionVerify(Context context) {
        if (isRunning) {
            Intent i = new Intent(context, PhoneNumberVerifier.class);
            i.setAction(ACTION_CANCEL);
            context.startService(i);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (smsReceiver == null) {
            smsReceiver = new SmsBrReceiver();
        }

        prefs = new PrefsHelper(this);
        smsRetrieverClient = SmsRetriever.getClient(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
        getApplicationContext().registerReceiver(smsReceiver, intentFilter);
        api = new ApiHelper(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        isVerifying = false;
        if (smsReceiver != null) {
            getApplicationContext().unregisterReceiver(smsReceiver);
            smsReceiver.cancelTimeout();
            smsReceiver = null;
        }
        if (!prefs.getVerified(false)) {
            // We're not verifying anything if this is stopped
            setVerificationState(null, false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final String action = intent.getAction();
        if (ACTION_VERIFY.equals(action)) {
            final String phoneNo = intent.getStringExtra(EXTRA_PHONE_NUMBER);
            startVerify(phoneNo);
        } else if (ACTION_VERIFY_MESSAGE.equals(action)) {
            String message = intent.getStringExtra(EXTRA_MESSAGE);
            verifyMessage(message);
        } else if (ACTION_CANCEL.equals(action)) {
            stopSelf();
        }

        // Redelivering intent because the server APIs are designed to be called multiple times
        return START_REDELIVER_INTENT;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not a bindable service");
    }

    public void notifyStatus(int status, @Nullable String phoneNo) {
        if (status < STATUS_STARTED || status >= STATUS_RESPONSE_VERIFIED) {
            isVerifying = false;
        } else {
            isVerifying = true;
        }

        try {
            Notification n = getNotification(this, status, phoneNo);
            startForeground(NOTIFICATION_ID, n);
            isRunning = true;
        } catch (IllegalStateException e) {
            Log.d(TAG, "Notification progress unable to be configured.");
        }
    }

    private void setVerificationState(@Nullable String phoneNumber, boolean verified) {
        prefs.setPhoneNumber(phoneNumber);
        prefs.setVerified(verified);
        Intent i = new Intent(ACTION_VERIFICATION_STATUS_CHANGE);
        LocalBroadcastManager.getInstance(PhoneNumberVerifier.this)
                .sendBroadcast(i);
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void startVerify(String phoneNo) {
        // Make this a foreground service
        notifyStatus(STATUS_STARTED, phoneNo);
        setVerificationState(phoneNo, false);

        // Start SMS receiver code
        Task<Void> task = smsRetrieverClient.startSmsRetriever();
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                smsReceiver.setTimeout();
                notifyStatus(STATUS_REQUEST_SENT, null);
                Log.d(TAG, "SmsRetrievalResult status: Success");
                Toast.makeText(PhoneNumberVerifier.this, getString(R.string.verifier_registered),
                        Toast.LENGTH_SHORT).show();
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "SmsRetrievalResult start failed.", e);
                stopSelf();
            }
        });


        // Communicate to background servers to send SMS and get the expect OTP
        notifyStatus(STATUS_REQUESTING, phoneNo);
        api.request(phoneNo,
                new ApiHelper.RequestResponse() {
                    @Override
                    public void onResponse(boolean success) {
                        if (success) {
                            Toast.makeText(PhoneNumberVerifier.this,
                                    getString(R.string.verifier_server_response),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Unsuccessful request call.");
                            Toast.makeText(PhoneNumberVerifier.this,
                                    getString(R.string.toast_unverified), Toast.LENGTH_LONG).show();
                            stopSelf();
                        }
                    }
                }, new ApiHelper.ApiError() {
                    @Override
                    public void onError(VolleyError error) {
                        // Do something else.
                        Log.d(TAG, "Error getting response");
                        Toast.makeText(PhoneNumberVerifier.this,
                                getString(R.string.toast_request_error), Toast.LENGTH_LONG).show();
                        stopSelf();
                    }
                });
    }

    private void verifyMessage(String smsMessage) {
        String requestPhone = prefs.getPhoneNumber("");
        notifyStatus(STATUS_RESPONSE_VERIFYING, null);
        api.verify(requestPhone, smsMessage,
                new ApiHelper.VerifyResponse() {
                    @Override
                    public void onResponse(boolean success, String phoneNumber) {
                        String requestPhone = prefs.getPhoneNumber("");
                        if (success && phoneNumber != null && phoneNumber.equals(requestPhone)) {
                            Log.d(TAG, "Successfully verified phone number: " + phoneNumber);
                            notifyStatus(STATUS_RESPONSE_VERIFIED, null);
                            Toast.makeText(PhoneNumberVerifier.this,
                                    getString(R.string.toast_verified), Toast.LENGTH_LONG).show();
                            setVerificationState(phoneNumber, true);
                        } else {
                            Log.d(TAG, "Unable to verify response.");
                            notifyStatus(STATUS_RESPONSE_FAILED, null);
                            Toast.makeText(PhoneNumberVerifier.this,
                                    getString(R.string.toast_unverified), Toast.LENGTH_LONG
                            ).show();
                        }
                        stopSelf();
                    }
                }, new ApiHelper.ApiError() {
                    @Override
                    public void onError(VolleyError error) {
                        Log.d(TAG, "Communication error with server.");
                        Toast.makeText(PhoneNumberVerifier.this,
                                getString(R.string.toast_unverified), Toast.LENGTH_LONG
                        ).show();
                        stopSelf();
                    }
                });
    }

    public static boolean isVerifying() {
        return isVerifying;
    }

    private Notification getNotification(Context context, int status, @Nullable String phoneNo) {
        if (phoneNo != null) {
            Intent cancelI = new Intent(this, PhoneNumberVerifier.class);
            cancelI.setAction(ACTION_CANCEL);
            PendingIntent cancelPI = PendingIntent.getService(this, 0, cancelI,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Action cancelA = new NotificationCompat.Action.Builder(
                    R.drawable.ic_not_interested_black_24dp,
                    getString(R.string.cancel_verification), cancelPI
            ).build();

            notification = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(context.getString(R.string.phone_verify_notify_title))
                    .setContentText(context.getString(R.string.phone_verify_notify_message, phoneNo))
                    .addAction(cancelA);
        } else if (notification == null) {
            throw new IllegalStateException("A Phone number needs to be provided initially");
        }

        notification.setProgress(STATUS_MAX, status, false);
        return notification.build();
    }

    class SmsBrReceiver extends BroadcastReceiver {
        Handler h = new Handler();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                doTimeout();
            }
        };

        public void setTimeout() {
            h.postDelayed(r, MAX_TIMEOUT);
        }

        public void cancelTimeout() {
            h.removeCallbacks(r);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(action)) {
                cancelTimeout();
                notifyStatus(STATUS_RESPONSE_RECEIVED, null);
                Bundle extras = intent.getExtras();
                Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                switch(status.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        String smsMessage = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                        Log.d(TAG, "Retrieved sms code: " + smsMessage);
                        if (smsMessage != null) {
                            verifyMessage(smsMessage);
                        }
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        doTimeout();
                        break;
                    default:
                        break;
                }
            }
        }

        private void doTimeout() {
            Log.d(TAG, "Waiting for sms timed out.");
            Toast.makeText(PhoneNumberVerifier.this,
                    getString(R.string.toast_unverified), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }
}
