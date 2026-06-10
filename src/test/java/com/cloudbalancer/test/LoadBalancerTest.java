package com.cloudbalancer.test;

import com.cloudbalancer.loadbalancer.FileRequest;
import com.cloudbalancer.loadbalancer.LoadBalancer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {

    @Test
    void testRoundRobinDistribution() {
        LoadBalancer lb = new LoadBalancer(Arrays.asList(
            "file-server-1", "file-server-2", "file-server-3", "file-server-4"));
        lb.setAlgorithm(LoadBalancer.Algorithm.ROUND_ROBIN);

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            FileRequest request = new FileRequest("file" + i, 1024, FileRequest.RequestType.UPLOAD, 1);
            seen.add(lb.selectContainer(request));
        }

        assertEquals(4, seen.size());
    }

    @Test
    void testAgingPreventsStarvation() {
        LoadBalancer lb = new LoadBalancer(Arrays.asList("file-server-1", "file-server-2"));
        lb.setAlgorithm(LoadBalancer.Algorithm.PRIORITY);

        FileRequest request = new FileRequest("lowpriority", 1024, FileRequest.RequestType.UPLOAD, 5);
        for (int i = 0; i < 6; i++) {
            lb.selectContainer(request);
        }

        assertTrue(request.getPriority() < 5);
    }
}
