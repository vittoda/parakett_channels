package com.flowstack.channels.slack;


import com.flowstack.channels.base.CommChannelBase;
import com.flowstack.channels.base.CommChannelInstance;

public class SlackChannel implements CommChannelBase {
    

    @Override
    public String getName() {
        return "Slack";
    }

    @Override
    public CommChannelInstance createInstance() {
        return new SlackChannelInstance();
    }

    @Override
    public String getKey() {
        return "slack";
    }

}
