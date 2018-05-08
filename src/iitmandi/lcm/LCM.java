package iitmandi.lcm;

import iitmandi.lcm.model.*;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;

public class LCM {

    public int totalConcepts;
    private Data originalData;
    private RoaringBitmap usefulObjectList;

    private ArrayList<Integer> removeObjects(Data data, Integer attributeID) {
        ArrayList<Integer> notRequiredObjectList = new ArrayList<>();
        for(Integer x: usefulObjectList) {
            if(!data.attributeMap.get(attributeID).objectList.contains(x)) {
                notRequiredObjectList.add(x);
            }
        }
        //Removing objects that do not contain attribute attributeID
        for(Integer x: notRequiredObjectList) {
            usefulObjectList.remove(x);
        }
        return notRequiredObjectList;
    }

    private RoaringBitmap findCommonAttributes(Data data){
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalAttributes + 1);
        for(Integer x: usefulObjectList) {
            result.and(data.objectMap.get(x).attributeList);
        }
        return result;
    }

    public void findClosedConcepts(Data data, RoaringBitmap seedConcept, int marker) {
        if(seedConcept.getCardinality() == 0) {
            totalConcepts = 0;
            usefulObjectList = new RoaringBitmap();
            usefulObjectList.add(1,data.totalObjects + 1);
            seedConcept = findCommonAttributes(data);
        }
        totalConcepts++;
//        System.out.println(totalConcepts);
//        System.out.println(seedConcept);
        if(marker > data.totalAttributes) {
            return;
        }
        for(int i=marker; i<=data.totalAttributes; i++){
            if(!seedConcept.contains(i)) {

                //Removing all objects that do not contain attribute i
                ArrayList<Integer> removedObjectList = removeObjects(data, i);
                RoaringBitmap childConcept = findCommonAttributes(data);

                boolean isDuplicate = false;
                for(Integer x: childConcept) {
                    if(x >= i) {
                        break;
                    }
                    if(!seedConcept.contains(x)) {
                        isDuplicate = true;
                    }
                }
                if(!isDuplicate) {
                    findClosedConcepts(data, childConcept, i + 1);
                }

                //BackTracking (Adding Removed Objects)
                for(Integer x: removedObjectList) {
                    usefulObjectList.add(x);
                }
            }
        }
    }
}