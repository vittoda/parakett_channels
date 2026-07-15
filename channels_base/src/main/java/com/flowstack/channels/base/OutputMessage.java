package com.flowstack.channels.base;

public class OutputMessage {

    private String _mText = null;
    private MessageContext _mContext = null;

    public OutputMessage(String text, MessageContext context) {
        this._mText = text;
        _mContext = context;
    }

    public String getText() {
        return _mText;
    }

    public MessageContext getContext() {
        return _mContext;
    }
    
    
}
