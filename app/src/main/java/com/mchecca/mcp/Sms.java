package com.mchecca.mcp;

import org.json.JSONObject;

import java.util.Date;

public class Sms {
    protected String number;
    protected String message;
    protected Type type;
    protected Date date;

    public enum Type {
        ALL, INBOX, SENT, DRAFT, OUTBOX, FAILED, QUEUED
    }
    public Sms(String number, String message, Type type, Date date) {
        this.number = number;
        this.message = message;
        this.type = type;
        this.date = date;
    }

    public String getNumber() {
        return number;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public JSONObject toJson() throws org.json.JSONException {
        JSONObject smsObj = new JSONObject();
        smsObj.put("number", number);
        smsObj.put("message", message);
        smsObj.put("type", type);
        smsObj.put("date", date);
        return smsObj;
    }
}
