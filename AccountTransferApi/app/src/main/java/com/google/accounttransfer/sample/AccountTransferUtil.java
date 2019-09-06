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
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

/**
 * Util class for Account Transfer
 */
public class AccountTransferUtil {

    public static final String ACCOUNT_TYPE = "com.accountransfer.account.type";
    public static final String KEY_ACCOUNT_NAME = "account_name";
    public static final String KEY_ACCOUNT_PASSWORD = "account_password";
    public static final String KEY_ACCOUNT_ARRAY = "account_array";

    private static final String TAG = "AccountTransferUtil";

    private AccountTransferUtil() {}

    static void importAccounts(byte[] transferBytes, Context context) throws JSONException {
        if (transferBytes != null) {
            String jsonString = new String(transferBytes, Charset.forName("UTF-8"));
            JSONObject object = new JSONObject(jsonString);
            JSONArray jsonArray = object.getJSONArray(KEY_ACCOUNT_ARRAY);
            for (int i = 0, size = jsonArray.length(); i < size; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String password = jsonObject.getString(KEY_ACCOUNT_PASSWORD);
                String name = jsonObject.getString(KEY_ACCOUNT_NAME);
                Account newAccount = new Account(
                        name + " COPY time:" + System.currentTimeMillis(), ACCOUNT_TYPE);
                boolean result = AccountManager.get(context)
                        .addAccountExplicitly(newAccount, password, null);
                // Don't log PII in actual app.
                Log.d(TAG, "Added account:" + newAccount + " with password:" + password
                        + " with result:" + result);
                Log.d(TAG, "Don't log PII in actual app");

            }
        }
    }

    static byte[] getTransferBytes(Context context) {
        AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(ACCOUNT_TYPE);
        String msg = "Length of accounts of type com.mfm are " + accounts.length;
        Log.v(TAG, msg);
        if (accounts.length != 0) {
            JSONArray jsonArray = new JSONArray();
            for (Account account : accounts) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put(KEY_ACCOUNT_NAME, account.name);
                    String password  = am.getPassword(account);
                    jsonObject.put(KEY_ACCOUNT_PASSWORD, password);
                } catch (JSONException e) {
                    Log.e(TAG, "Error while creating bytes for transfer", e);
                    return null;
                }
                jsonArray.put(jsonObject);
            }
            JSONObject object = new JSONObject();
            try {
                object.put(KEY_ACCOUNT_ARRAY, jsonArray);
            } catch (JSONException e) {
                Log.e(TAG, "Error", e);
                return null;
            }
            return object.toString().getBytes(Charset.forName("UTF-8"));
        }
        return null;
    }
}
