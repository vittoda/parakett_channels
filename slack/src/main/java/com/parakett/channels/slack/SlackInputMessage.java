package com.parakett.channels.slack;

import com.parakett.channels.base.InputMessage;
import com.parakett.channels.base.MessageContext;

public class SlackInputMessage extends InputMessage{

    private String _mUserId= null;
    private String _mChannel = null;
    private String _mThreadId = null;

    public SlackInputMessage(String text, String userId, String channel, String threadId) {
        super(text);
        _mUserId = userId;
        _mChannel = channel;
        _mThreadId = threadId;
    }

    public String getUserId() {
        return _mUserId;
    }

    public String getChannel() {
        return _mChannel;
    }

    public MessageContext getContext() {
        return new SlackMessageContext(_mChannel, _mUserId, _mThreadId);
    }
    
}
