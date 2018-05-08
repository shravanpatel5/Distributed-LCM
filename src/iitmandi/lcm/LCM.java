package iitmandi.lcm;

import iitmandi.lcm.model.*;
import iitmandi.lcm.model.Object;
import org.roaringbitmap.RoaringBitmap;

public class LCM {

    public int totalConcepts;
    private Data originalData;
    private RoaringBitmap usefulObjectList;

    private void makeCopy(Data data) {
        originalData = new Data();
        originalData.totalObjects = data.totalObjects;
        originalData.totalAttributes = data.totalAttributes;
        for(Integer x: data.objectMap.keySet()) {
            Object object = new Object(x);
            object.attributeList = data.objectMap.get(x).attributeList.clone();
            originalData.objectMap.put(x, object);
        }
        for(Integer x: data.attributeMap.keySet()) {
            Attribute attribute = new Attribute(x);
            attribute.objectList = data.attributeMap.get(x).objectList.clone();
            originalData.attributeMap.put(x, attribute);
        }
    }

    private RoaringBitmap removeObjects(Data data, Integer attributeID) {

        RoaringBitmap notRequiredObjectList = new RoaringBitmap();

//        for(Integer x: data.objectMap.keySet()) {
//            notRequiredObjectList.add(x);
//        }
        for(Integer x: usefulObjectList) {
            notRequiredObjectList.add(x);
        }
        for(Integer x: data.attributeMap.get(attributeID).objectList) {
            notRequiredObjectList.remove(x);
        }

        //Removing objects that do not contain attribute attributeID
//        for(Integer x: notRequiredObjectList) {
//            for(Integer y: data.objectMap.get(x).attributeList) {
//                data.attributeMap.get(y).objectList.remove(x);
//            }
//        }
//        for(Integer x: notRequiredObjectList) {
//            data.objectMap.remove(x);
//        }
        for(Integer x: notRequiredObjectList) {
            usefulObjectList.remove(x);
        }
        return notRequiredObjectList;
    }

    private RoaringBitmap findCommonAttributes(Data data){
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalAttributes + 1);
//        for (Integer objectID : data.objectMap.keySet()) {
//            result.and(data.objectMap.get(objectID).attributeList);
//        }
        for(Integer x: usefulObjectList) {
            result.and(data.objectMap.get(x).attributeList);
        }
        return result;
    }

    public void findClosedConcepts(Data data, RoaringBitmap seedConcept, int marker) {
        if(seedConcept.getCardinality() == 0) {
            totalConcepts = 0;
//            makeCopy(data);
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
                RoaringBitmap removedObjectList = removeObjects(data, i);
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
//                for(Integer x: removedObjectList) {
//                    data.objectMap.put(x, originalData.objectMap.get(x));
//                }
//                for(Integer x: removedObjectList) {
//                    for(Integer y: data.objectMap.get(x).attributeList) {
//                        data.attributeMap.get(y).objectList.add(x);
//                    }
//                }
            }
        }
    }
}