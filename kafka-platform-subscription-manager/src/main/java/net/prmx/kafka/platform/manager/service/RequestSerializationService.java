package net.prmx.kafka.platform.manager.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * T042: Service to serialize concurrent requests per subscriber
 * Uses per-subscriber locks to ensure sequential processing
 */
@Service
public class RequestSerializationService {

    private final ConcurrentHashMap<String, ReentrantLock> subscriberLocks = new ConcurrentHashMap<>();

    /**
     * Execute a task serialized for a specific subscriber
     * Requests for the same subscriberId will be processed sequentially
     * Requests for different subscriberIds can execute in parallel
     */
    public void executeSerializedForSubscriber(String subscriberId, Runnable task) {
        ReentrantLock lock = subscriberLocks.computeIfAbsent(subscriberId, k -> new ReentrantLock());
        
        lock.lock();
        try {
            task.run();
        } finally {
            lock.unlock();
        }
    }
}
