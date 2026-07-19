package com.flowstack.channels.gmail;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.flowstack.channels.base.CommChannelBase;
import com.flowstack.channels.base.CommChannelInstance;

public class GmailChannel implements CommChannelBase  {

    private HashMap<String, GmailChannelInstance> _mInstances = new HashMap<>();

    @Override
    public String getName() {
        return "Gmail";
    }

    @Override
    public String getKey() {
        return "gmail";
    }

    @Override
    public CommChannelInstance createInstance(String key, JsonNode config) {
        GmailChannelInstance instance =  new GmailChannelInstance(config);
        _mInstances.put(key, instance);
        return instance;
    }

    @Override
    public CommChannelInstance getInstance(String key) {
       return _mInstances.get(key);
    }
    
}
