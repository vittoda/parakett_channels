package com.flowstack.channels.base;

public class CommChannelException extends Exception {

    public CommChannelException(String e) {
        super(e);
    }

    public CommChannelException(Exception e) {
        super(e);
    }
    
}
