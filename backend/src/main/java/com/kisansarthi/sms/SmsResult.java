package com.kisansarthi.sms;

public class SmsResult {

    private final boolean success;
    private final String sid;
    private final String status;
    private final Integer errorCode;
    private final String errorMessage;

    public SmsResult(boolean success, String sid, String status, Integer errorCode, String errorMessage) {
        this.success = success;
        this.sid = sid;
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public static SmsResult accepted(String sid, String status) {
        return new SmsResult(true, sid, status != null ? status : "ACCEPTED", null, null);
    }

    public static SmsResult failed(Integer errorCode, String errorMessage) {
        return new SmsResult(false, null, "FAILED", errorCode, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSid() {
        return sid;
    }

    public String getStatus() {
        return status;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "SmsResult{" +
                "success=" + success +
                ", sid='" + sid + '\'' +
                ", status='" + status + '\'' +
                ", errorCode=" + errorCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
