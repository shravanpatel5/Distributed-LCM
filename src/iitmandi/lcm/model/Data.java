package iitmandi.lcm.model;

import java.util.ArrayList;

public class Data {
    public Integer totalObjects;
    public Integer totalAttributes;
    public ArrayList<Object> objectMap;
    public ArrayList<Attribute> attributeMap;

    public Data() {
        objectMap = new ArrayList<>();
        attributeMap = new ArrayList<>();
    }
}