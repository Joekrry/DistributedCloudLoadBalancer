package com.cloudbalancer.manager;

import com.cloudbalancer.mqtt.MQTTClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.io.*;

public class HostManager {

    private static final String SCALE_TOPIC  = "cloudbalancer/scale";
    private static final String HEALTH_TOPIC = "cloudbalancer/health";

    private MQTTClient mqttClient;

    public HostManager() {
        try {
            mqttClient = new MQTTClient("host-manager");
            mqttClient.subscribe(SCALE_TOPIC,  this::handleScaleCommand);
            mqttClient.subscribe(HEALTH_TOPIC, this::handleHealthCheck);
            System.out.println("Host Manager started.");
        } catch (Exception e) {
            System.err.println("Host Manager init failed: " + e.getMessage());
        }
    }

    private void handleScaleCommand(String topic, MqttMessage message) {
        String command = new String(message.getPayload());
        if (command.startsWith("START:")) {
            startContainer(command.substring(6));
        } else if (command.startsWith("STOP:")) {
            stopContainer(command.substring(5));
        }
    }

    private void handleHealthCheck(String topic, MqttMessage message) {
        String containerName = new String(message.getPayload());
        boolean healthy = checkContainerHealth(containerName);
        try {
            mqttClient.publish(HEALTH_TOPIC + "/response",
                containerName + ":" + (healthy ? "HEALTHY" : "UNHEALTHY"));
        } catch (Exception e) {
            System.err.println("Health response failed: " + e.getMessage());
        }
    }

    public void startContainer(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "-d",
                "--name", containerName,
                "--network", "cloudbalancer_cb-network",
                "ubuntu:latest",
                "bash", "-c",
                "apt-get update && apt-get install -y openssh-server && " +
                "mkdir /var/run/sshd && echo 'root:fileserver' | chpasswd && " +
                "sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && " +
                "/usr/sbin/sshd -D"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            logOutput(process);
            System.out.println("Container " + containerName + " started (exit " + process.waitFor() + ")");
        } catch (Exception e) {
            System.err.println("Failed to start container: " + e.getMessage());
        }
    }

    public void stopContainer(String containerName) {
        try {
            runDockerCommand("docker", "stop", containerName);
            runDockerCommand("docker", "rm",   containerName);
            System.out.println("Container " + containerName + " stopped and removed.");
        } catch (Exception e) {
            System.err.println("Failed to stop container: " + e.getMessage());
        }
    }

    public boolean checkContainerHealth(String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "inspect", "--format", "{{.State.Running}}", containerName);
            Process process = pb.start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                .readLine();
            return "true".equals(output);
        } catch (Exception e) {
            return false;
        }
    }

    private void runDockerCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        logOutput(pb.start());
    }

    private void logOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) System.out.println("[Docker] " + line);
    }

    public MQTTClient getMqttClient() { return mqttClient; }
}
