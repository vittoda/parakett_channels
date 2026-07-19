package com.flowstack.channels.gmail;

import com.flowstack.channels.base.MessageContext;

public class GmailMessageContext extends MessageContext {

    public String messageId = null;
    public String senderEmail = null;
    public String threadId = null;
    public String subject = null;

    public GmailMessageContext(String subject, String messageId, String senderEmail, String threadId) {
        this.subject = subject;
        this.messageId = messageId;
        this.senderEmail = senderEmail;
        this.threadId = threadId;
    }
    
}
