package org.dromara.modules.monitor.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 对接 prometheus
 *
 * @author Lion Li
 */
@Slf4j
@RestController
@RequestMapping("/actuator/prometheus")
public class PrometheusController {

    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 从注册中心获取所有服务组装成 prometheus 的数据结构
     */
    @GetMapping("/sd")
    public List<Map<String, Object>> sd() {
        List<String> services = discoveryClient.getServices();
        if (services == null || services.isEmpty()) {
            return new ArrayList<>(0);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (String service : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(service);
            List<String> targets = instances.stream().map(i -> i.getHost() + ":" + i.getPort()).collect(Collectors.toList());

            Map<String, String> labels = new HashMap<>(2);
            // 数据来源(区分异地使用)
            // labels.put("__meta_datacenter", "beijing");
            // 服务名
            labels.put("__meta_prometheus_job", service);
            Map<String, Object> group = new HashMap<>(2);
            group.put("targets", targets);
            group.put("labels", labels);
            list.add(group);
        }
        return list;
    }

    /**
     * 接收 prometheus 报警消息
     *
     * @param message 消息体
     */
    @PostMapping("/alerts")
    public ResponseEntity<Void> alerts(@RequestBody String message) {
        log.info("[prometheus] alert =>" + message);
        return ResponseEntity.ok().build();
    }

}
