/*
 * Copyright (c) 2021 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.iotdevice;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import eu.hansolo.evt.EvtObserver;
import eu.hansolo.evt.EvtType;
import eu.hansolo.iotmodules.IotModule;
import eu.hansolo.iotmodules.PropertyManager;
import eu.hansolo.iotmodules.event.MqttEvt;
import eu.hansolo.iotmodules.event.MqttEvtObserver;
import eu.hansolo.iotmodules.event.SensorEvt;
import eu.hansolo.iotmodules.event.SwitchEvt;
import eu.hansolo.iotmodules.event.TemperatureSensorEvt;
import eu.hansolo.iotmodules.mqtt.MqttManager;
import eu.hansolo.iotmodules.sensors.Sensor;
import eu.hansolo.iotmodules.tools.Constants;
import eu.hansolo.properties.ChangeListener;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static eu.hansolo.iotmodules.tools.Constants.COLON;
import static eu.hansolo.iotmodules.tools.Constants.COMMA;
import static eu.hansolo.iotmodules.tools.Constants.CURLY_BRACKET_CLOSE;
import static eu.hansolo.iotmodules.tools.Constants.CURLY_BRACKET_OPEN;
import static eu.hansolo.iotmodules.tools.Constants.FIELD_MODULE_ID;
import static eu.hansolo.iotmodules.tools.Constants.FIELD_SENSOR_ID;
import static eu.hansolo.iotmodules.tools.Constants.FIELD_TIMESTAMP;
import static eu.hansolo.iotmodules.tools.Constants.FIELD_TYPE;
import static eu.hansolo.iotmodules.tools.Constants.FIELD_VALUE;
import static eu.hansolo.iotmodules.tools.Constants.MQTT_ACTORS_TOPIC;
import static eu.hansolo.iotmodules.tools.Constants.MQTT_IOT_MODULES_TOPIC;
import static eu.hansolo.iotmodules.tools.Constants.MQTT_PLUS;
import static eu.hansolo.iotmodules.tools.Constants.MQTT_SENSORS_TOPIC;
import static eu.hansolo.iotmodules.tools.Constants.MQTT_TOPIC_SEPARATOR;
import static eu.hansolo.iotmodules.tools.Constants.QUOTES;


public class Pizw extends IotModule implements EvtObserver<SensorEvt> {
    private Context                  pi4j;
    private ScheduledExecutorService service;
    private Led                      led;
    private PushButton               button;
    private ChangeListener<Boolean>  connectionListener;
    private MqttEvtObserver          mqttEvtObserver;


    public Pizw() {
        super();
        pi4j               = Pi4J.newAutoContext();
        service            = Executors.newScheduledThreadPool(1);
        led                = new Led("1", pi4j, 13, 19, 26);
        button             = new PushButton("1", pi4j, 14);
        connectionListener = evt -> {
            if (null == mqttManager) { return; }
            final String presenceTopic = new StringBuilder().append(PropertyManager.INSTANCE.getString(Constants.PROPERTY_MQTT_TOPIC_PRESENCE))
                                                            .append(MQTT_TOPIC_SEPARATOR)
                                                            .append(PropertyManager.INSTANCE.getString(Constants.PROPERTY_CLIENT_ID))
                                                            .toString();
            final String msg = toJsonString();
            mqttManager.publish(presenceTopic, MqttQos.AT_LEAST_ONCE, true, msg);

            final String actorTopic = new StringBuilder().append(MQTT_IOT_MODULES_TOPIC).append(MQTT_TOPIC_SEPARATOR).append(getId()).append(MQTT_TOPIC_SEPARATOR).append(MQTT_ACTORS_TOPIC).append(MQTT_TOPIC_SEPARATOR).append(MQTT_PLUS).toString();
            if (evt.getValue()) {
                // Subscribe to topics
                mqttManager.subscribe(actorTopic, MqttQos.AT_MOST_ONCE);
            } else {
                // Unsubscribe from topcis
                mqttManager.unsubscribe(actorTopic);
            }
        };
        mqttEvtObserver    = evt -> receive(evt);

        init();
        registerListeners();
        initMqttManager();
    }


    public void init() {
        sensors.add(button);
        actors.add(led);
    }

    public void registerListeners() {
        connected.addListener(connectionListener);
        button.addOnEvt(SwitchEvt.ANY, this);
    }

    private void initMqttManager() {
        mqttManager = new MqttManager(toJsonString());
        connected.bind(mqttManager.connectedProperty());
        mqttManager.addMqttObserver(mqttEvtObserver);
    }


    public void publish(final SensorEvt evt) {
        if (null == mqttManager) { return; }
        String topic = "";
        String msg   = "";
        if (evt instanceof TemperatureSensorEvt) {
            Sensor sensor = (Sensor) evt.getSource();
            topic = new StringBuilder().append(MQTT_IOT_MODULES_TOPIC).append(MQTT_TOPIC_SEPARATOR).append(getId()).append(MQTT_TOPIC_SEPARATOR).append(MQTT_SENSORS_TOPIC).append(MQTT_TOPIC_SEPARATOR).append(sensor.getSensorType().getTypeId()).append(MQTT_TOPIC_SEPARATOR).append(sensor.getId()).toString();
            msg   = new StringBuilder().append(CURLY_BRACKET_OPEN)
                                       .append(QUOTES).append(FIELD_MODULE_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                       .append(QUOTES).append(FIELD_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(sensor.getSensorType().name()).append(QUOTES).append(COMMA)
                                       .append(QUOTES).append(FIELD_SENSOR_ID).append(QUOTES).append(COLON).append(sensor.getId()).append(COMMA)
                                       .append(QUOTES).append(FIELD_VALUE).append(QUOTES).append(COLON).append(((TemperatureSensorEvt) evt).getTemperature()).append(COMMA)
                                       .append(QUOTES).append(FIELD_TIMESTAMP).append(QUOTES).append(COLON).append(Instant.now().getEpochSecond())
                                       .append(CURLY_BRACKET_CLOSE)
                                       .toString();
        } else if (evt instanceof SwitchEvt) {
            Sensor sensor = (Sensor) evt.getSource();
            topic = new StringBuilder().append(MQTT_IOT_MODULES_TOPIC).append(MQTT_TOPIC_SEPARATOR).append(getId()).append(MQTT_TOPIC_SEPARATOR).append(MQTT_SENSORS_TOPIC).append(MQTT_TOPIC_SEPARATOR).append(sensor.getSensorType().getTypeId()).append(MQTT_TOPIC_SEPARATOR).append(sensor.getId()).toString();
            msg   = new StringBuilder().append(CURLY_BRACKET_OPEN)
                                       .append(QUOTES).append(FIELD_MODULE_ID).append(QUOTES).append(COLON).append(QUOTES).append(getId()).append(QUOTES).append(COMMA)
                                       .append(QUOTES).append(FIELD_TYPE).append(QUOTES).append(COLON).append(QUOTES).append(sensor.getSensorType().name()).append(QUOTES).append(COMMA)
                                       .append(QUOTES).append(FIELD_SENSOR_ID).append(QUOTES).append(COLON).append(sensor.getId()).append(COMMA)
                                       .append(QUOTES).append(FIELD_VALUE).append(QUOTES).append(COLON).append(((SwitchEvt) evt).isOn()).append(COMMA)
                                       .append(QUOTES).append(FIELD_TIMESTAMP).append(QUOTES).append(COLON).append(Instant.now().getEpochSecond())
                                       .append(CURLY_BRACKET_CLOSE)
                                       .toString();
        }

        if (topic.isBlank() || msg.isBlank()) { return; }
        mqttManager.publish(topic, MqttQos.AT_MOST_ONCE, false, msg);
    }

    public void receive(final MqttEvt evt) {
        if (null == evt) { return; }
        final String topic = evt.getTopic();
        final String msg   = evt.getMsg();
        System.out.println(topic + " => " + msg);
    }


    @Override public void reboot() {
        if (null != pi4j) { pi4j.shutdown(); }
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("sudo reboot");
            System.exit(0);
        } catch (IOException e) {

        }
    }

    @Override public void shutdown() {
        if (null != pi4j) { pi4j.shutdown(); }
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("sudo shutdown -h now");
            System.exit(0);
        } catch (IOException e) {

        }
    }

    @Override public void handle(final SensorEvt event) {
        EvtType<? extends SensorEvt> type = event.getEvtType();
        if (type.equals(TemperatureSensorEvt.TEMPERATURE)) {
            if (null == mqttManager) { return; }
            publish(event);
        } else if (type.equals(SwitchEvt.ANY)) {
            publish(event);
        }
    }
}
