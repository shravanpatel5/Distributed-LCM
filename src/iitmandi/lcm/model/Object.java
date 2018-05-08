package iitmandi.lcm.model;

import org.roaringbitmap.RoaringBitmap;

public class Object {
    public Integer ID;
    public RoaringBitmap attributeList;

    public Object(Integer ID) {
        this.ID = ID;
        attributeList = new RoaringBitmap();
    }
}