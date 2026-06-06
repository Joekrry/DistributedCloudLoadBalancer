package com.cloudbalancer.loadbalancer;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

public class LoadBalancer {

    public enum Algorithm { FCFS, ROUND_ROBIN, PRIORITY, SJN }

    private Algorithm currentAlgorithm = Algorithm.ROUND_ROBIN;
    private final CopyOnWriteArrayList<String> activeContainers;
    private final CopyOnWriteArrayList<Semaphore> containerLocks;
    private int roundRobinIndex = 0;
    private final Random random = new Random();

    public LoadBalancer(List<String> containers) {
        this.activeContainers = new CopyOnWriteArrayList<>(containers);
        this.containerLocks = new CopyOnWriteArrayList<>();
        for (int i = 0; i < containers.size(); i++) {
            containerLocks.add(new Semaphore(1));
        }
    }

    public synchronized String selectContainer(FileRequest request) {
        switch (currentAlgorithm) {
            case FCFS:        return fcfsSelect();
            case PRIORITY:    return prioritySelect(request);
            case SJN:         return sjnSelect();
            default:          return roundRobinSelect();
        }
    }

    private String fcfsSelect() {
        for (int i = 0; i < activeContainers.size(); i++) {
            if (containerLocks.get(i).tryAcquire()) {
                containerLocks.get(i).release();
                return activeContainers.get(i);
            }
        }
        return activeContainers.get(0);
    }

    private String roundRobinSelect() {
        String container = activeContainers.get(roundRobinIndex % activeContainers.size());
        roundRobinIndex = (roundRobinIndex + 1) % activeContainers.size();
        return container;
    }

    private String prioritySelect(FileRequest request) {
        request.incrementWaitTime();
        // aging: boost priority after waiting too long to prevent starvation
        if (request.getWaitTime() > 5) request.boostPriority();
        return roundRobinSelect();
    }

    private String sjnSelect() {
        int bestIndex = 0;
        int minLoad = Integer.MAX_VALUE;
        for (int i = 0; i < containerLocks.size(); i++) {
            int load = containerLocks.get(i).availablePermits() == 0 ? 1 : 0;
            if (load < minLoad) { minLoad = load; bestIndex = i; }
        }
        return activeContainers.get(bestIndex);
    }

    public void simulateLatency() {
        try {
            Thread.sleep(1000 + random.nextInt(4000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void acquireContainerLock(String containerName) throws InterruptedException {
        int index = activeContainers.indexOf(containerName);
        if (index >= 0) containerLocks.get(index).acquire();
    }

    public void releaseContainerLock(String containerName) {
        int index = activeContainers.indexOf(containerName);
        if (index >= 0) containerLocks.get(index).release();
    }

    public void addContainer(String containerName) {
        activeContainers.add(containerName);
        containerLocks.add(new Semaphore(1));
    }

    public void removeContainer(String containerName) {
        int index = activeContainers.indexOf(containerName);
        if (index >= 0) {
            activeContainers.remove(index);
            containerLocks.remove(index);
        }
    }

    public List<String> getActiveContainers() {
        return Collections.unmodifiableList(activeContainers);
    }

    public int getBusyCount() {
        int busy = 0;
        for (Semaphore s : containerLocks) {
            if (s.availablePermits() == 0) busy++;
        }
        return busy;
    }

    public void setAlgorithm(Algorithm algorithm) { this.currentAlgorithm = algorithm; }
    public Algorithm getAlgorithm() { return currentAlgorithm; }
}
