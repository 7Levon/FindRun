//package com.av.avmessenger;
//
//public class msgModelclass {
//    String message;
//    String senderid;
//    long timeStamp;
//
//    public msgModelclass() {
//    }
//
//    public msgModelclass(String message, String senderid, long timeStamp) {
//        this.message = message;
//        this.senderid = senderid;
//        this.timeStamp = timeStamp;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//
//    public void setMessage(String message) {
//        this.message = message;
//    }
//
//    public String getSenderid() {
//        return senderid;
//    }
//
//    public void setSenderid(String senderid) {
//        this.senderid = senderid;
//    }
//
//    public long getTimeStamp() {
//        return timeStamp;
//    }
//
//    public void setTimeStamp(long timeStamp) {
//        this.timeStamp = timeStamp;
//    }
//}


package com.example.findrun;

public class msgModelclass {
    private String message;
    private String senderid;
    private long timestamp;
    private boolean isRead;

    public msgModelclass() {}

    public msgModelclass(String message, String senderid, long timestamp) {
        this.message = message;
        this.senderid = senderid;
        this.timestamp = timestamp;
        this.isRead = false;  // Default to false
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderid() {
        return senderid;
    }

    public void setSenderid(String senderid) {
        this.senderid = senderid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
