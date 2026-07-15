package com.flowstack.channels.slack;

import java.util.LinkedList;
import java.util.List;

import com.flowstack.channels.base.CommChannelException;
import com.flowstack.channels.base.CommChannelInstance;
import com.flowstack.channels.base.MessageContext;
import com.flowstack.channels.base.OnMessageHandler;
import com.flowstack.channels.base.OutputMessage;

public class SlackChannelInstance implements CommChannelInstance {

    private List<OnMessageHandler> _mMessageHandlers = new LinkedList<>();
    private SlackConnection _mConnection = null;

    @Override
    public void sendMessage(OutputMessage msg) throws CommChannelException {
        SlackMessageContext ctx = (SlackMessageContext) msg.getContext();
        _mConnection.sendMessage(ctx.channelId, msg.getText(), ctx.threadId);
    }

    @Override
    public void registerOnMessageHandler(OnMessageHandler handler) {
        _mMessageHandlers.add(handler);
    }

    @Override
    public void initialize() throws CommChannelException {
        _mConnection = new SlackConnection();
        _mConnection.initialize();
        _mConnection.setMessageReceiveHandler(new MessageReceiveHandler() {
            public void onMessageReceived(String message, String userId, String channel, String threadId) {
                for (OnMessageHandler handler : _mMessageHandlers) {
                    SlackInputMessage sm = new SlackInputMessage(message, userId, channel, threadId);
                    OutputMessage om = handler.onMessageReceived(sm);
                    if (om != null) {
                        _mConnection.sendMessage(channel, om.getText(), threadId);
                    }
                }

            }
        });
    }

     /*
     * This will be used for Human In Loop messages from agent. The message will need to be responded in the same thread. 
       This one later can be changed to a proper workflow in slack.
     */
    @Override
    public void getConfirmationResponse(MessageContext context, String message, String responseKey, String requestId) throws CommChannelException {
        SlackMessageContext slackContext = (SlackMessageContext)context;
        _mConnection.triggerBlockForGettingResponse(slackContext.channelId, message,requestId,responseKey);
    }


   
}
