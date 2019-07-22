package com.java.nexyd.otp.interfaces;

public interface OTPDialogListener {
    void onOTPCodeEntered(boolean status, String code);
    void onCancelOTP();
}