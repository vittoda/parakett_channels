package com.parakett.channels.base;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private JsonUtils() {
        // prevent instantiation
    }
    
}
