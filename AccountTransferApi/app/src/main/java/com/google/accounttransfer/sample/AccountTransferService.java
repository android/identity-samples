/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.accounttransfer.sample;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.auth.api.accounttransfer.AccountTransfer;
import com.google.android.gms.auth.api.accounttransfer.AccountTransferClient;
import com.google.android.gms.auth.api.accounttransfer.AuthenticatorTransferCompletionStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AccountTransferService extends IntentService {

    private static final String TAG = "AccountTransferService";
    private static final String ACTION_START_ACCOUNT_EXPORT =
            AccountTransfer.ACTION_START_ACCOUNT_EXPORT;
    private static final String ACCOUNT_TYPE = AccountTransferUtil.ACCOUNT_TYPE;

    private static final String ACCOUNT_TRANSFER_CHANNEL = "TRANSFER ACCOUNTS";
    private static final int NOTIFICATION_ID = 1;

    private static final long TIMEOUT_API = 10;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    public AccountTransferService() {
        super("AccountTransferService");
    }

    public static Intent getIntent(Context context, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setClass(context, AccountTransferService.class);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            createNotificationChannel();
            builder = new Notification.Builder(this, ACCOUNT_TRANSFER_CHANNEL);
        } else {
            builder = new Notification.Builder(this);
        }
        Notification notification = builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(getString(R.string.copying_text))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.copying_text))
                .setContentText(getString(R.string.copying_text))
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "Receiver with action == null");
            return;
        }
        Log.d(TAG, "Receiver with action:" + action);

        switch (action) {

            case AccountTransfer.ACTION_ACCOUNT_IMPORT_DATA_AVAILABLE:
                importAccount();
                return;

            case ACTION_START_ACCOUNT_EXPORT:
            case AccountTransfer.ACTION_ACCOUNT_EXPORT_DATA_AVAILABLE:
                exportAccount();
                return;

            default:
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void importAccount() {
        // Handle to client object
        AccountTransferClient client = AccountTransfer.getAccountTransferClient(this);

        // Make RetrieveData api call to get the transferred over data.
        Task<byte[]> transferTask = client.retrieveData(ACCOUNT_TYPE);
        try {
            byte[] transferBytes = Tasks.await(transferTask, TIMEOUT_API, TIME_UNIT);
            AccountTransferUtil.importAccounts(transferBytes, this);
        } catch (ExecutionException | InterruptedException | TimeoutException | JSONException e) {
            Log.e(TAG, "Exception while calling importAccounts()", e);
            client.notifyCompletion(
                    ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_FAILURE);
            return;
        }
        client.notifyCompletion(
                ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_SUCCESS);

    }

    private void exportAccount() {
        Log.d(TAG, "exportAccount()");
        byte[] transferBytes = AccountTransferUtil.getTransferBytes(this);
        AccountTransferClient client = AccountTransfer.getAccountTransferClient(this);
        if (transferBytes == null) {
            Log.d(TAG, "Nothing to export");
            // Notifying is important.
            client.notifyCompletion(
                    ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_SUCCESS);
            return;
        }

        // Send the data over to the other device.
        Task<Void> exportTask = client.sendData(ACCOUNT_TYPE, transferBytes);
        try {
            Tasks.await(exportTask, TIMEOUT_API, TIME_UNIT);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Exception while calling exportAccounts()", e);
            // Notifying is important.
            client.notifyCompletion(
                    ACCOUNT_TYPE, AuthenticatorTransferCompletionStatus.COMPLETED_FAILURE);
            return;
        }
    }

    private void createNotificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String id = ACCOUNT_TRANSFER_CHANNEL;
        CharSequence name = "AccountTransfer";
        String description = "Account Transfer";
        int importance = NotificationManager.IMPORTANCE_MIN;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setDescription(description);
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mNotificationManager.createNotificationChannel(mChannel);
    }
}