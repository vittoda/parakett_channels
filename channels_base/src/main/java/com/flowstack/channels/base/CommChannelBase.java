package com.flowstack.channels.base;

public interface CommChannelBase {

    public String getName();
    public String getKey();
    public CommChannelInstance createInstance();
    
}
