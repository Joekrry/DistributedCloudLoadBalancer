package com.cloudbalancer.mqtt;

import org.eclipse.paho.client.mqttv3.*;

public class MQTTClient {
    private static final String BROKER = "tcp://mqtt-broker:1883";
    private MqttClient client;

    public MQTTClient(String clientId) throws MqttException {
        client = new MqttClient(BROKER, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        client.connect(options);
    }

    public void publish(String topic, String message) throws MqttException {
        MqttMessage msg = new MqttMessage(message.getBytes());
        msg.setQos(1);
        client.publish(topic, msg);
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        client.subscribe(topic, listener);
    }

    public void disconnect() throws MqttException {
        if (client.isConnected()) client.disconnect();
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}
