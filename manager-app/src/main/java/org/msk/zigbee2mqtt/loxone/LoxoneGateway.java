package org.msk.zigbee2mqtt.loxone;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.msk.zigbee2mqtt.MqttService;
import org.msk.zigbee2mqtt.ZigbeeService;
import org.msk.zigbee2mqtt.configuration.DeviceConfiguration;
import org.msk.zigbee2mqtt.configuration.DeviceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoxoneGateway {

    @Value("${MQTT_TO_LOXONE_TOPIC}")
    private String MQTT_TO_LOXONE_TOPIC;

    private final ZigbeeService zigbeeService;
    private final MqttService mqttService;
    private final ObjectMapper objectMapper;
    private final DeviceConfiguration deviceConfiguration;
    private List<Listener> listeners = new ArrayList<>();
    // todo dusan.zatkovsky config
    private boolean loxoneStuffEnabled = true;

    @PostConstruct
    private void init() throws ExecutionException, InterruptedException {
        if(!loxoneStuffEnabled) {
            return;
        }
        Assert.notNull(MQTT_TO_LOXONE_TOPIC, "You must configure MQTT_TO_LOXONE_TOPIC environment variable");
        deviceConfiguration.load();
        zigbeeService.addZigbeeDeviceMessageListener(this::processMessage);
    }

    private void processMessage(String deviceName, byte[] message) {
        try {
            HashMap json = objectMapper.readValue(message, HashMap.class);
            processMap(deviceName, "", json);
        } catch (Exception e) {
            log.error("Failed to deserialize message", e);
        }
    }

    private void processMap(String deviceName, String path, Map json) {
        json.keySet().forEach(key -> {
            Object value = json.get(key);
            if (value instanceof Map) {
                processMap(deviceName, format("%s/%s", path, key.toString()), (Map) value);
            } else if (value instanceof Collection) {
                ((Collection) value).forEach(i -> {
                    sendToLoxone(deviceName, format("%s/%s", path, key.toString()), i.toString());
                });
            } else {
                sendToLoxone(deviceName, format("%s/%s", path, key.toString()), value.toString());
            }
        });

    }

    private void sendToLoxone(String deviceName, String path, String value) {
        path = normalize(path);
        for(Listener l : listeners) {
            l.apply(deviceName, path, value);
        }
        value = translateToLoxone(deviceName, path, value);
        String loxoneMsg = format("zigbee/%s/%s %s", deviceName, path, value);
        mqttService.publish(MQTT_TO_LOXONE_TOPIC, loxoneMsg);
        log.debug("Sent '{}' to loxone", loxoneMsg);
    }

    private String translateToLoxone(String deviceName, String path, String value) {
        DeviceType deviceType = zigbeeService.getDeviceType(deviceName);
        if(deviceType == null ) {
            return value;
        }
        return deviceConfiguration.translateValueForward(deviceType, path, value);
    }

    private String normalize(String key) {
        while (key.contains("//")) {
            key = key.replaceAll("//", "/");
        }
        return key.startsWith("/") ? key.replaceFirst("/","") : key;
    }

    public interface Listener {
        void apply(String deviceName, String path, String value);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

}
