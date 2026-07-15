package com.flowstack.channels.slack;

public interface MessageReceiveHandler {

    public void onMessageReceived(String message, String userId, String channel, String threadId);
    
}
