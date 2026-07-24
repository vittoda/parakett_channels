package com.parakett.channels.slack;

import com.fasterxml.jackson.databind.JsonNode;
import com.parakett.channels.base.CommChannelException;
import com.parakett.channels.base.CommChannelInstance;
import com.parakett.channels.base.MessageContext;
import com.parakett.channels.base.OnMessageHandler;
import com.parakett.channels.base.OutputMessage;

public class SlackChannelInstance implements CommChannelInstance {

    private OnMessageHandler _mMessageHandler = null;
    private SlackConnection _mConnection = null;
    private JsonNode _mConfig = null;

    public SlackChannelInstance(JsonNode config) {
        _mConfig = config;
    }

    @Override
    public void sendMessage(OutputMessage msg) throws CommChannelException {
        SlackMessageContext ctx = (SlackMessageContext) msg.getContext();
        _mConnection.sendMessage(ctx.channelId, msg.getText(), ctx.threadId);
    }

    @Override
    public void registerOnMessageHandler(OnMessageHandler handler) {
        _mMessageHandler = handler;
    }

    @Override
    public void initialize() throws CommChannelException {
        _mConnection = new SlackConnection(_mConfig);
        _mConnection.initialize();
        _mConnection.setMessageReceiveHandler(new MessageReceiveHandler() {
            public void onMessageReceived(String message, String userId, String channel, String threadId) {
                if (_mMessageHandler != null) {
                    SlackInputMessage sm = new SlackInputMessage(message, userId, channel, threadId);
                    OutputMessage om = _mMessageHandler.onMessageReceived(sm);
                    if (om != null) {
                        _mConnection.sendMessage(channel, om.getText(), threadId);
                    }
                }

            }
        });
    }

    /*
     * This will be used for Human In Loop messages from agent. The message will
     * need to be responded in the same thread.
     * This one later can be changed to a proper workflow in slack.
     */
    @Override
    public void getConfirmationResponse(MessageContext context, String message, String responseKey, String requestId)
            throws CommChannelException {
        SlackMessageContext slackContext = (SlackMessageContext) context;
        _mConnection.triggerBlockForGettingResponse(slackContext.channelId, message, requestId, responseKey);
    }

}
