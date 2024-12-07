package net.gamedoctor.PBServer.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class CoreMessage {
    private String action;
    private String data;

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>();
        map.put("action", this.action);
        map.put("data", this.data);
        return map;
    }
}