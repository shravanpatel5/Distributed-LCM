package iitmandi.lcm.model;

import org.roaringbitmap.RoaringBitmap;

public class Node {
    public RoaringBitmap seedConcept;
    public Integer marker;

    public Node(RoaringBitmap seedConcept, Integer marker) {
        this.seedConcept = seedConcept;
        this.marker = marker;
    }
    public Node(int[] array) {
        marker = array[0];
        seedConcept = new RoaringBitmap();
        for(Integer i = 1; i < array.length; i++) {
            seedConcept.add(array[i]);
        }
    }
    public String toString() {
        return "seedConcept: " + this.seedConcept + ", marker: " + this.marker;
    }
    public int[] toArray() {
        int[] result = new int[1 + seedConcept.getCardinality()];
        result[0] = marker;
        int cnt = 1;
        for(Integer x: seedConcept) {
            result[cnt++] = x;
        }
        return result;
    }
}
