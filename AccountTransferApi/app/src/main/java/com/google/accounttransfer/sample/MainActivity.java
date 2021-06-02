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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.accounttransfer.AccountTransfer;
import com.google.android.gms.auth.api.accounttransfer.AccountTransferClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SHARED_PREF = "accountTransfer";
    private static final String KEY_FIRST_TIME = "first_time";
    private static final String ACCOUNT_TYPE = AccountTransferUtil.ACCOUNT_TYPE;

    private static Boolean firstTime = null;
    SharedPreferences sharedPreferences;
    private TextView mAccountTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAccountTextView = (TextView) findViewById(R.id.account_text);
        findViewById(R.id.refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                populateAccountTextView();
            }
        });
        // In production app, call this in a background thread, showing progress screen meanwhile.
        fetchData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateAccountTextView();
    }

    private void populateAccountTextView() {
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        String accountString = "Accounts of type " + ACCOUNT_TYPE + " are : \n";
        if (accounts.length != 0) {
            for (Account account : accounts) {
                accountString += "Account:" +  account.name + "\n";
            }
        } else {
            accountString = "No Accounts of type " + ACCOUNT_TYPE +
                    " found. Please add accounts before exporting.";
            mAccountTextView.setTextColor(Color.RED);
        }
        mAccountTextView.setText(accountString);
    }

    private void fetchData() {
        // The logic can be modified according to your need.
        sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        firstTime = sharedPreferences.getBoolean(KEY_FIRST_TIME, true);
        if (firstTime) {
            Log.v(TAG, "Fetching account info from API");
            AccountTransferClient client = AccountTransfer.getAccountTransferClient(this);
            final Task<byte[]> transferBytes = client.retrieveData(ACCOUNT_TYPE);
            transferBytes.addOnCompleteListener(this, new OnCompleteListener<byte[]>() {
                @Override
                public void onComplete(@NonNull Task<byte[]> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Success retrieving data. Importing it.");
                        try {
                            AccountTransferUtil.importAccounts(task.getResult(), MainActivity.this);
                        } catch (JSONException e) {
                            Log.e(TAG, "Encountered failure while retrieving data", e);
                            return;
                        }
                        populateAccountTextView();
                    } else {
                        // No need to notify API about failure, as it's running outside setup of
                        // device.
                        Log.e(TAG, "Encountered failure while retrieving data", task.getException());
                    }
                    // Don't try the next time
                    sharedPreferences.edit().putBoolean(KEY_FIRST_TIME, false).apply();
                }
            });
        }
    }
}
