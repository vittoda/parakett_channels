package com.flowstack.channels.base;

public class InputMessage {

    private String _mText = null;

    public InputMessage(String text) {
        _mText = text;
    }

    public String getText() {
        return _mText;
    }

    public MessageContext getContext() {
        return null;
    }
    
}
