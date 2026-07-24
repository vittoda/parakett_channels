This module provides framework for Parakett channel implementation. Some of key points

* For every channel, you need to implement *CommChannelBase* and *CommChannelInstance*.
	* *CommChannelBase* : Class implenenting this interface provides definition for the channel. There will be only one instance of this in Parakett, shared by all agents.
	* *CommChannelInstance* : The class implementing this interface, will handle all the messaging. There can be more than one instance in Parakett server. Mostly one instance per agent that needs this channel. Instance is created using the class implementing *CommChannelBase*. Along with regular event, output message, each channel can provide a customized output, like confirmation message, which may trigger a special handling on the channel implementation.
* Agents or the clients will implement *OnMessageHandler*, and register with the channel instance. When an event/message is recieved, it will call this handler. 
* Message Context will be available with Input message. One of the usecase for this context is when sending the message back on the channel, to maintain threa.


## CLI channel
While agent needs to define communication channels, it needs to use, CLI channel will be added by default for all channels. The CLI channel is part of the Parakett server implementation.

## Build
To build *channel_base* and publish to local maven repo, use the following command.

`./gradlew clean build publishToMavenLocal`
