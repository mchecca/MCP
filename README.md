# MCP
This is the Android application for MCP.

### Compiling
Running `gradlew assembleDebug` is all that is required to build.

### Configuration
MCP needs to be able to reach an MQTT broker. The MQTT server input field allows you to set the hostname, port, and optionally username/password for the MQTT connection using the following format: [username:password@]mqtt_ip:mqtt_port.
