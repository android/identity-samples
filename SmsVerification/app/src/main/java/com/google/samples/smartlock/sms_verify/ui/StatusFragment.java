package com.google.samples.smartlock.sms_verify.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.samples.smartlock.sms_verify.R;

/**
 * Created by pmatthews on 11/18/16.
 */
public class StatusFragment extends Fragment implements MainActivity.StatusReciever {
    private static final String TAG = StatusFragment.class.getSimpleName();
    private StatusUi ui;
    private String lastPhoneNumber;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_status, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ui = new StatusUi(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ui != null) {
            ui.setVerified(lastPhoneNumber);
        }
    }

    @Override
    public void onStatusUpdate(@Nullable String phoneNumber) {
        lastPhoneNumber = phoneNumber;
        if (ui != null) {
            ui.setVerified(phoneNumber);
        }
    }

    class StatusUi implements View.OnClickListener {
        private Button resetBtn;
        private Button signUpBtn;
        private TextView status;

        public StatusUi(View layout) {
            signUpBtn = (Button) layout.findViewById(R.id.sign_up_btn);
            signUpBtn.setOnClickListener(this);

            status = (TextView) layout.findViewById(R.id.status);
        }

        public void setVerified(@Nullable String phoneNo) {
            Context context = getContext();
            if (status != null && context != null) {
                String text = context.getString(R.string.status_text);
                if (phoneNo != null) {
                    text = context.getString(R.string.status_text_template, phoneNo);
                }
                status.setText(text);
            }
        }

        @Override
        public void onClick(View view) {
            if (view.equals(signUpBtn)) {
                Intent i = new Intent(getContext(), SignUpActivity.class);
                startActivity(i);
            } else if (view.equals(resetBtn)) {
                Intent i = new Intent(getContext(), ResetActivity.class);
                startActivity(i);
            }
        }
    }
}
