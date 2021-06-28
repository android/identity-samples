package com.google.samples.smartlock.sms_verify.ui;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.samples.smartlock.sms_verify.PhoneNumberVerifier;
import com.google.samples.smartlock.sms_verify.R;

/**
 * Created by pmatthews on 11/18/16.
 */
public class VerifyingFragment extends Fragment {

    private VerifyingUi ui;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_verifiying, container, false);
        ui = new VerifyingUi(view);
        return view;
    }

    private void cancelVerify() {
        Context context = getContext();
        PhoneNumberVerifier.stopActionVerify(context);
        Toast.makeText(context, getString(R.string.toast_cancelled_verification),
                Toast.LENGTH_LONG).show();
    }

    private void verifyPhoneNumber(String message) {
        PhoneNumberVerifier.startActionVerifyMessage(getContext(), message);
    }

    class VerifyingUi implements View.OnClickListener {
        private Button submitBtn;
        private Button cancelBtn;
        private EditText field;

        public VerifyingUi(View layout) {
            cancelBtn = (Button) layout.findViewById(R.id.verifying_menu_verification_cancel);
            cancelBtn.setOnClickListener(this);

            submitBtn = (Button) layout.findViewById(R.id.verifying_menu_verification_submit);
            submitBtn.setOnClickListener(this);

            field = (EditText) layout.findViewById(R.id.verifying_menu_verification_field);
        }

        @Override
        public void onClick(View view) {
            if (view.equals(cancelBtn)) {
                cancelVerify();
            } else if (view.equals(submitBtn)) {
                String fieldText = getFieldText();
                if (fieldText == null || fieldText.isEmpty()) {
                    // Don't submit an empty text field.
                    Toast.makeText(getContext(),
                            getString(R.string.verifying_menu_verification_required_text),
                            Toast.LENGTH_LONG).show();
                } else {
                    verifyPhoneNumber(fieldText);
                }
            }
        }

        public String getFieldText() {
            if (field != null) {
                if (field.getText() != null) {
                    return field.getText().toString();
                }
            }
            return null;
        }
    }
}
