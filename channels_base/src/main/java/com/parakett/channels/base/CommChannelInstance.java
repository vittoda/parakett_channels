package com.parakett.channels.base;

public interface CommChannelInstance {
    public  void sendMessage(OutputMessage msg) throws CommChannelException;
    public  void registerOnMessageHandler(OnMessageHandler handler);
    public  void initialize() throws CommChannelException;
    public void getConfirmationResponse(MessageContext context, String message, String respnseKey, String requestId) throws CommChannelException ;
   
}
