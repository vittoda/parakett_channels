package com.parakett.channels.slack;


import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.parakett.channels.base.CommChannelBase;
import com.parakett.channels.base.CommChannelInstance;

public class SlackChannel implements CommChannelBase {

    private HashMap<String, SlackChannelInstance> _mChannelInstances = new HashMap<>();
    
    @Override
    public String getName() {
        return "Slack";
    }

    @Override
    public CommChannelInstance createInstance(String key, JsonNode config) {
        SlackChannelInstance instance =  new SlackChannelInstance(config);
        _mChannelInstances.put(key, instance);
        return instance;
    }

    @Override
    public String getKey() {
        return "slack";
    }

    @Override
    public CommChannelInstance getInstance(String key) {
        return _mChannelInstances.get(key);
    }

}
