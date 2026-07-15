# Slack channel for FlowStack
This module provides Slack channel implementation for FlowStack. You need to setup the authentication and authorization on the slack to recieve messages. Auth tokens *botToken* and *appToken* needs to be defined in ~/.fskeys. Use key *slack.channel.config* and attributes *botToken* and *appToken* . 

## Build
Make sure the *channel_base* is build before building Slack channel implementation.

`./graddlew clean build publishToMavenLocal`
