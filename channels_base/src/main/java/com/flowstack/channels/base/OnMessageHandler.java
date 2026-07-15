package com.flowstack.channels.base;

public interface OnMessageHandler {
    public OutputMessage onMessageReceived(InputMessage msg); 
}
