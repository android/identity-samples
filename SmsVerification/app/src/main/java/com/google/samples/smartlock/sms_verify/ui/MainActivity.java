package com.google.samples.smartlock.sms_verify.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.samples.smartlock.sms_verify.PhoneNumberVerifier;
import com.google.samples.smartlock.sms_verify.PrefsHelper;
import com.google.samples.smartlock.sms_verify.R;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private PrefsHelper prefs;
    private String verifiedPhoneNo;
    private MainUi ui;
    private VerificationStatusChangeReceiver verificationReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = new PrefsHelper(this);
        verificationReceiver = new VerificationStatusChangeReceiver();
        ui = new MainUi(findViewById(R.id.activity_main), new StatusFragment(),
                new VerifyingFragment());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PhoneNumberVerifier.ACTION_VERIFICATION_STATUS_CHANGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(verificationReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (verificationReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(verificationReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    protected void updateStatus() {
        boolean isVerified = prefs.getVerified(false);
        verifiedPhoneNo = prefs.getPhoneNumber(null);
        ui.setFragment(!isVerified && PhoneNumberVerifier.isVerifying());
        ui.notifyStatus(verifiedPhoneNo);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.menu_reset_verification:
                i = new Intent(this, ResetActivity.class);
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    class VerificationStatusChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (PhoneNumberVerifier.ACTION_VERIFICATION_STATUS_CHANGE.equals(intent.getAction())) {
                updateStatus();
            }
        }
    }

    interface StatusReciever {
        public void onStatusUpdate(@Nullable String phoneNumber);
    }

    class MainUi {
        private FragmentManager manager;
        private FrameLayout container;
        private Fragment statusFrag;
        private Fragment validationFrag;

        public MainUi(View view, Fragment statusFragment, Fragment validationFragment) {
            manager = getSupportFragmentManager();
            container = (FrameLayout) view.findViewById(R.id.main_fragment_container);
            statusFrag = statusFragment;
            validationFrag = validationFragment;
        }

        public void notifyStatus(@Nullable String status) {
            if (statusFrag instanceof StatusReciever) {
                ((StatusReciever) statusFrag).onStatusUpdate(status);
            }
            if (validationFrag instanceof StatusReciever) {
                ((StatusReciever) validationFrag).onStatusUpdate(status);
            }
        }

        public void setFragment(boolean isVerifying) {
            Fragment current = getCurrentFragment();
            Fragment frag = statusFrag;
            if (isVerifying) {
                frag = validationFrag;
            }
            if (current == null || frag.getId() != current.getId()) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(container.getId(), frag);
                transaction.commitAllowingStateLoss();
            }
        }

        public Fragment getCurrentFragment() {
            return manager.findFragmentById(container.getId());
        }
    }
}
