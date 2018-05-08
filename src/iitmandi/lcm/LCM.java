package iitmandi.lcm;

import iitmandi.lcm.model.*;
import org.roaringbitmap.RoaringBitmap;

public class LCM {

    public int totalConcepts;

    private RoaringBitmap findCommonObjects(Data data, RoaringBitmap attributeList) {
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalObjects + 1);
        if(attributeList.getCardinality() != 0) {
            for (Integer attributeID : attributeList) {
                result.and(data.attributeMap.get(attributeID).objectList);
            }
        }
        return result;
    }

    private RoaringBitmap findCommonAttributes(Data data, RoaringBitmap objectList){
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalAttributes + 1);
        if(objectList.getCardinality() != 0) {
            for (Integer objectID : objectList) {
                result.and(data.objectMap.get(objectID).attributeList);
            }
        }
        return result;
    }

    public void findClosedConcepts(Data data, RoaringBitmap seedConcept, int marker) {
        if(seedConcept.getCardinality() == 0) {
            totalConcepts = 0;
            seedConcept = findCommonAttributes(data, findCommonObjects(data, seedConcept));
        }
        totalConcepts++;
//        System.out.println(seedConcept);
        if(marker > data.totalAttributes) {
            return;
        }
        for(int i=marker; i<=data.totalAttributes; i++){
            if(!seedConcept.contains(i)) {
                seedConcept.add(i);
                RoaringBitmap childConcept = findCommonAttributes(data, findCommonObjects(data, seedConcept));
                seedConcept.remove(i);

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
            }
        }
    }
}