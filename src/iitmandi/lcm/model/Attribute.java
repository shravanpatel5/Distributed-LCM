package iitmandi.lcm.model;

import org.roaringbitmap.RoaringBitmap;

public class Attribute {
    public Integer ID;
    public RoaringBitmap objectList;

    public Attribute(Integer ID) {
        this.ID = ID;
        objectList = new RoaringBitmap();
    }
}
