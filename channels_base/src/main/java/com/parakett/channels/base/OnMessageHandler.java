package com.parakett.channels.base;

public interface OnMessageHandler {
    public OutputMessage onMessageReceived(InputMessage msg); 
}
