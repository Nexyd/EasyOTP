package com.java.nexyd.otp.dialogs;

import android.content.DialogInterface;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.indra.mobile.otp.R;
import com.indra.mobile.otp.interfaces.OTPDialogListener;
import com.indra.mobile.otp.interfaces.OTPListener;
import com.indra.mobile.otp.receivers.SMSBroadcastReceiver;

public class OTPDialog
    extends DialogFragment
    implements OTPListener
{
    private static boolean otpHasExpired;
    private boolean otpSended;
    private String code;
    private OTPDialogListener listener;
    private OTPListener smsListener;
    private EditText[] digits;
    private TextView errorMsg;
    // private CountDownTimer otpTimer;

    @Override
    public void onCreate(Bundle mSavedInstanceState) {
        super.onCreate(mSavedInstanceState);
        otpSended = false;
        smsListener = this;

        OTPDialog.otpHasExpired = false;
//        otpTimer = new CountDownTimer(300000, 60000) {
//            public void onTick(long millisUntilFinished) {}
//            public void onFinish() {
//                OTPDialog.otpHasExpired = true;
//            }
//        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
        ViewGroup container, Bundle savedInstanceState) {

        getDialog().getWindow().setBackgroundDrawable(
            new ColorDrawable(Color.TRANSPARENT));

        digits = new EditText[6];
        View view = inflater.inflate(R.layout.otp_dialog, container);
        Button cancelOTP = view.findViewById(R.id.cancel);
        errorMsg = view.findViewById(R.id.otpErrorMessage);

        digits[0] = view.findViewById(R.id.otp_digit_1);
        digits[1] = view.findViewById(R.id.otp_digit_2);
        digits[2] = view.findViewById(R.id.otp_digit_3);
        digits[3] = view.findViewById(R.id.otp_digit_4);
        digits[4] = view.findViewById(R.id.otp_digit_5);
        digits[5] = view.findViewById(R.id.otp_digit_6);
        addListeners();

        errorMsg.setVisibility(View.GONE);
        cancelOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // otpTimer.cancel();
                listener.onCancelOTP();
                dismiss();
            }
        });

        // otpTimer.start();
        startSmsListener();

        return view;
    }

    private void addListeners() {
        for (int i = 0; i < digits.length; i++) {
            final int index = i;
            digits[i].setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {

                    boolean result = false;
                    if (keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DEL) &&
                        (digits[index].length() == 0) &&
                        (index > 0))
                    {
                        digits[index - 1].setText("");
                        digits[index - 1].requestFocus();

                        result = true;
                    }

                    return result;
                }
            });

            digits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if ((index + 1 < digits.length) && (digits[index].length() == 1))
                        digits[index + 1].requestFocus();
                    else if (index - 1 >= 0)
                        digits[index - 1].requestFocus();

                    if (digits[index].hasFocus() || digits[index].length() > 0)
                        digits[index].setBackground(getResources().getDrawable(
                            R.drawable.otp_digit_box_border_filled));
                    else
                        digits[index].setBackground(getResources().getDrawable(
                            R.drawable.otp_digit_box_border_empty));

                    if (buildOTPCode().length() == 6)
                        sendOTPCode();
                }

                @Override
                public void afterTextChanged(Editable editable) {}
            });
        }
    }

    private String buildOTPCode() {
        StringBuilder otpCode = new StringBuilder();

        if (digits[0].getText() != null)
            otpCode.append(digits[0].getText().toString());

        if (digits[1].getText() != null)
            otpCode.append(digits[1].getText().toString());

        if (digits[2].getText() != null)
            otpCode.append(digits[2].getText().toString());

        if (digits[3].getText() != null)
            otpCode.append(digits[3].getText().toString());

        if (digits[4].getText() != null)
            otpCode.append(digits[4].getText().toString());

        if (digits[5].getText() != null)
            otpCode.append(digits[5].getText().toString());

        code = otpCode.toString().isEmpty()?
            "" : otpCode.toString();

        return code;
    }

    private void startSmsListener() {

        // Get an instance of SmsRetrieverClient, used to start listening for a matching SMS message.
        SmsRetrieverClient client = SmsRetriever.getClient(getContext());

        // Starts SmsRetriever, which waits for ONE matching SMS message until timeout
        // (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
        // action SmsRetriever#SMS_RETRIEVED_ACTION.
        Task<Void> task = client.startSmsRetriever();

        // Listen for success/failure of the start Task. If in a background thread, this
        // can be made blocking using Tasks.await(task, [timeout]);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Successfully started retriever, expect broadcast intent
                SMSBroadcastReceiver broadcastReceiver =
                    new SMSBroadcastReceiver(smsListener);

                getContext().registerReceiver(broadcastReceiver,
                    new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION));

                Toast.makeText(getContext(),
                    getString(R.string.waiting_otp),
                    Toast.LENGTH_LONG).show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Failed to start retriever, inspect Exception for more details
                Toast.makeText(getContext(),
                    getString(R.string.otp_not_started),
                    Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendOTPCode() {
        String otpCode = buildOTPCode();
        if (!otpCode.isEmpty() && otpCode.length() == 6) {
            code = otpCode;
            otpSended = true;
            dismiss();

        } else {
            errorMsg.setVisibility(View.VISIBLE);
        }
    }

    public void setListener(OTPDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOTPReceived(String otp) {
        String[] otpDigits = otp.split("");
        for (int i = 0; i < digits.length; i++)
            digits[i].setText(otpDigits[i]);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        listener.onOTPCodeEntered(otpSended, code);
    }
}