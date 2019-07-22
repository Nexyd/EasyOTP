package com.java.nexyd.otp.dialogs;

import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
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

public class OTPDialogBasic
    extends DialogFragment
    implements OTPListener
{
    private boolean otpSended;
    private EditText otpCode;
    private String code;
    private OTPDialogListener listener;
    private OTPListener smsListener;
    private int otpLength;

    @Override
    public void onCreate(Bundle mSavedInstanceState) {
        super.onCreate(mSavedInstanceState);
        otpSended = false;
        smsListener = this;
        otpLength = 8;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
        ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.otp_dialog_basic, container);
        otpCode = view.findViewById(R.id.otp_code);
        final TextView errorMsg = view.findViewById(R.id.otpErrorMessage);
        Button sendOTP = view.findViewById(R.id.sendOTP);

        errorMsg.setVisibility(View.GONE);
        sendOTP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable otp = otpCode.getText();
                if (otp != null && otp.length() == otpLength) {
                    sendOTPCode();
                } else {
                    errorMsg.setVisibility(View.VISIBLE);
                }
            }
        });

        startSmsListener();

        return view;
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
        otpSended = true;
        code = otpCode.getText() == null? "" :
            otpCode.getText().toString();

        dismiss();
    }

    public void setListener(OTPDialogListener listener) {
        this.listener = listener;
    }

    @Override
    public void onOTPReceived(String otp) {
        otpCode.setText(otp);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        listener.onOTPCodeEntered(otpSended, code);
    }
}