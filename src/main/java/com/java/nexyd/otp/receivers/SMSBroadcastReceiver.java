package com.java.nexyd.otp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import com.indra.mobile.otp.R;
import com.indra.mobile.otp.interfaces.OTPListener;


/**
 * BroadcastReceiver to wait for SMS messages. This can be registered either
 * in the AndroidManifest or at runtime.  Should filter Intents on
 * SmsRetriever.SMS_RETRIEVED_ACTION.
 */
public class SMSBroadcastReceiver
    extends  BroadcastReceiver
{
    private OTPListener listener;

    public SMSBroadcastReceiver() {}
    public SMSBroadcastReceiver(OTPListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

            switch(status.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    String message = (String) extras.get(
                        SmsRetriever.EXTRA_SMS_MESSAGE);

                    String otpCode = extractOTPCodeFrom(context, message);
                    listener.onOTPReceived(otpCode);

                    break;

                case CommonStatusCodes.TIMEOUT: // (5 minutes)
                    Toast.makeText(context,
                        context.getString(R.string.otp_timeout),
                        Toast.LENGTH_LONG).show();

                    break;
            }
        }
    }

    public String extractOTPCodeFrom(Context context, String sms) {

        String otpCode = "";
        if (sms.startsWith("<#>")) {
//            String target = "<#>" + context.getString(R.string.otp_header);
//            sms = sms.replace(target, "");
            String[] smsCode = sms.split(" ");
            otpCode = smsCode[0];
        }

        return otpCode;
    }
}
