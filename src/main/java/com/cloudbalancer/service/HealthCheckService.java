package com.cloudbalancer.service;

import com.cloudbalancer.loadbalancer.LoadBalancer;
import com.cloudbalancer.manager.HostManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HealthCheckService {

    private final HostManager hostManager;
    private final LoadBalancer loadBalancer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public HealthCheckService(HostManager hostManager, LoadBalancer loadBalancer) {
        this.hostManager = hostManager;
        this.loadBalancer = loadBalancer;
    }

    public void startPeriodicChecks(int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            for (String container : loadBalancer.getActiveContainers()) {
                boolean healthy = hostManager.checkContainerHealth(container);
                if (!healthy) {
                    System.err.println("Container " + container + " unhealthy — restarting.");
                    loadBalancer.removeContainer(container);
                    hostManager.startContainer(container);
                    loadBalancer.addContainer(container);
                }
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }
}
