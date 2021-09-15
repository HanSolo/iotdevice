module eu.hansolo.iotdevice {
    // Java
    requires java.base;

    // 3rd Party
    requires eu.hansolo.iotmodules;
    requires eu.hansolo.evt;
    requires eu.hansolo.properties;
    requires com.hivemq.client.mqtt;
    requires com.google.gson;
    requires com.pi4j;

    requires jdk.unsupported;

    exports eu.hansolo.iotdevice;
}