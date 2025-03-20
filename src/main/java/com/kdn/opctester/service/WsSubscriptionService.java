package com.kdn.opctester.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Service
public class WsSubscriptionService {
    private final OpcService opcService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<Integer, ScheduledFuture<?>> pollingTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public WsSubscriptionService(OpcService opcService, SimpMessagingTemplate messagingTemplate) {
        this.opcService =opcService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        String destination = SimpMessageHeaderAccessor.wrap(event.getMessage()).getDestination();
        if(destination != null && destination.startsWith("/topic/opcua/")) {
            String idStr = destination.substring("/topic/opcua/".length());
            try {
                Integer id = Integer.parseInt(idStr);
                startPollingForId(id);
            } catch(NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    @EventListener
    public void handleSessionUnsubscribeEvent(SessionUnsubscribeEvent event) {
        String destination = SimpMessageHeaderAccessor.wrap(event.getMessage()).getDestination();
        if(destination != null && destination.startsWith("/topic/opcua/")) {
            String idStr = destination.substring("/topic/opcua/".length());
            try {
                Integer id = Integer.parseInt(idStr);
                stopPollingForId(id);
            } catch(NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void startPollingForId(Integer id) {
        if(pollingTasks.containsKey(id)) {
            return;
        }
        System.out.println("============================");
        System.out.println("Start polling for id: " + id);
        System.out.println("============================");
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                Map<String,Map<String,Object>> values = opcService.pollInternal();
                System.out.println("======================");
                System.out.println("poll internal: " + values.size());
                if(values.size() > 0) {
                    System.out.println(values.get(0));
                }
                System.out.println("======================");
                messagingTemplate.convertAndSend("/topic/opcua/" + id, values);
                opcService.enqueue(values);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);

        pollingTasks.put(id, task);
    }

    private void stopPollingForId(Integer id) {
        ScheduledFuture<?> task = pollingTasks.get(id);
        if(task != null) {
            task.cancel(false);
            pollingTasks.remove(id);
        }
    }
}
