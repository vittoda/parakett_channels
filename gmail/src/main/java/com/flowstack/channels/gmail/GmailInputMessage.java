package com.flowstack.channels.gmail;

import com.flowstack.channels.base.InputMessage;
import com.flowstack.channels.base.MessageContext;

public class GmailInputMessage extends InputMessage {

    private GmailMessageContext _mContext = null;

    public GmailInputMessage(String text, GmailMessageContext context) {
        super(text);
        _mContext = context;
    }

    @Override
    public MessageContext getContext() {
        return _mContext;
    }


    @Override
    public String getText() {
        return "Subject : "+ _mContext.subject+"\nSender : "+_mContext.senderEmail+"\nBody : \n"+super.getText();
    }
    
}
