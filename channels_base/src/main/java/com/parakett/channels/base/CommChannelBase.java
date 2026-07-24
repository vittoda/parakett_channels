package com.parakett.channels.base;

import com.fasterxml.jackson.databind.JsonNode;

public interface CommChannelBase {

    /**
     * Returns a display name of channel
     * @return A human readable name for the channel
     */
    public String getName();

    /**
     * Returns a uniqueue key for the channel. This key will be used to identfy the channel in agent definition.
     * @return A unique key for the channel name. An alphanumeric value, and starts with a letter.
     */
    public String getKey();
    /**
     * Create a new channel instance for a given key. Here the key will mostly be an agent Id. If an instance with
     * same key already exists, it will be replaced and a new instance will be giveb.
     * @param key Key of the instance to be created.
     * @param config Configuration for the instance
     * @return A channel instance
     */
    public CommChannelInstance createInstance(String key, JsonNode config);


    /**
     * Return a channel instance for a given key. 
     * @param key Key for the channel instance.
     * @return Channel instance for the key if it exists, null otherwise
     */
    public CommChannelInstance getInstance(String key);
    
}
