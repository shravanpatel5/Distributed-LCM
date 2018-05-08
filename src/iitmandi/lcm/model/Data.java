package iitmandi.lcm.model;

import java.util.HashMap;
import java.util.Map;

public class Data {
    public Integer totalObjects;
    public Integer totalAttributes;
    public Map<Integer, Object> objectMap;
    public Map<Integer, Attribute> attributeMap;

    public Data() {
        objectMap = new HashMap<>();
        attributeMap = new HashMap<>();
    }
}