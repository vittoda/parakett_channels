package com.parakett.channels.slack;

import com.parakett.channels.base.MessageContext;

public class SlackMessageContext extends MessageContext{

    public String channelId = null;
    public String userId = null;
    public String threadId = null;

    public SlackMessageContext(String channelId, String userId, String threadId) {
        this.channelId = channelId;
        this.userId = userId;
        this.threadId = threadId;
    }
    
}
