package iitmandi.lcm;

import iitmandi.lcm.model.*;
import mpi.MPI;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class LCM {


    public int totalConcepts;
    public static double startTime;
    private Data data;
    private Queue<Node> queue;
    private ArrayList<ArrayList<Integer>> T;

    public LCM(Data data) {
        this.data = data;
        queue = new LinkedList<>();
        T = new ArrayList<>();
        for(Integer i = 0; i <= data.totalAttributes; i++) {
            T.add(new ArrayList<>());
        }
        for(Integer i = 1; i <= data.totalObjects; i++) {
            T.get(0).add(i);
        }
    }

//    private ArrayList<Integer> removeObjects(Integer attributeID) {
//        ArrayList<Integer> notRequiredObjectList = new ArrayList<>();
//        for(Integer x: usefulObjectList) {
//            if(!data.attributeMap.get(attributeID).objectList.contains(x)) {
//                notRequiredObjectList.add(x);
//            }
//        }
//        //Removing objects that do not contain attribute attributeID
//        for(Integer x: notRequiredObjectList) {
//            usefulObjectList.remove(x);
//        }
//        return notRequiredObjectList;
//    }

//    private RoaringBitmap findCommonAttributes(){
//        RoaringBitmap result = new RoaringBitmap();
//        result.add(1, data.totalAttributes + 1);
//        for(Integer x: usefulObjectList) {
//            result.and(data.objectMap.get(x).attributeList);
//        }
//        return result;
//    }

    private RoaringBitmap findCommonObjects(RoaringBitmap attributeList) {
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalObjects + 1);
        if(attributeList.getCardinality() != 0) {
            for (Integer attributeID : attributeList) {
                result.and(data.attributeMap.get(attributeID).objectList);
            }
        }
        return result;
    }

    private RoaringBitmap findCommonAttributes(RoaringBitmap objectList){
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalAttributes + 1);
        if(objectList.getCardinality() != 0) {
            for (Integer objectID : objectList) {
                result.and(data.objectMap.get(objectID).attributeList);
            }
        }
        return result;
    }

    private RoaringBitmap findCommonAttributes(ArrayList<Integer> objectList){
        RoaringBitmap result = new RoaringBitmap();
        result.add(1, data.totalAttributes + 1);
        if(objectList.size() != 0) {
            for (Integer objectID : objectList) {
                result.and(data.objectMap.get(objectID).attributeList);
            }
        }
        return result;
    }

    private Node generateChild(Node node) {
        boolean validConcept = true;
        if (!node.seedConcept.contains(node.marker)) {
            node.seedConcept.add(node.marker);
            RoaringBitmap objectList = findCommonObjects(node.seedConcept);
            RoaringBitmap childConcept = findCommonAttributes(objectList);
            node.seedConcept.remove(node.marker);
            for (Integer x : childConcept) {
                if (x >= node.marker) {
                    break;
                }
                if (!node.seedConcept.contains(x)) {
                    validConcept = false;
                }
            }
            if(validConcept) {
                return new Node(childConcept, node.marker + 1);
            }
        }
        return null;
    }

    public void distributeLoad() {
        System.out.println("Distribution Started");
        Integer totalComputers = MPI.COMM_WORLD.Size();
        Queue<Node> queue = new LinkedList<>();
        RoaringBitmap rootConcept = findCommonAttributes(findCommonObjects(new RoaringBitmap()));
        Node root = new Node(rootConcept,1);
        queue.add(root);

        if(totalComputers == 1) {
            this.queue.add(root);
            System.out.println("Distribution Completed: " + (System.nanoTime() - startTime )/1000000000.0);
            return;
        }

        int[] size = new int[1];

        while(!queue.isEmpty() && (data.totalAttributes - queue.peek().marker) >= 1) {
            Integer sizeOfQueue = queue.size();
            for(Integer i = 0; i < sizeOfQueue; i++) {
                Node node = queue.remove();
                Node child = generateChild(node);
                node.marker++;
                queue.add(node);
                if(child != null) {
                    queue.add(child);
                }
            }
            if(queue.size() >= totalComputers) {
                for (Integer i = 0; i < totalComputers; i++) {
                    Node node = queue.remove();
                    int[] array = node.toArray();
                    size[0] = array.length;
                    if(i == 0) {
                        this.queue.add(node);
                    }
                    else {
                        MPI.COMM_WORLD.Send( size, 0, 1, MPI.INT, i, 0);
                        MPI.COMM_WORLD.Send( array, 0, array.length, MPI.INT, i, 0);
                    }
                }
            }
        }

        size[0] = 0;
        for (Integer i = 1; i < totalComputers; i++) {
            MPI.COMM_WORLD.Send( size, 0, 1, MPI.INT, i, 0);
        }
        System.out.println("Distribution Completed: " + (System.nanoTime() - startTime )/1000000000.0);

        while(!queue.isEmpty()) {
            this.queue.add(queue.remove());
        }
    }

    public void receiveLoad() {
        int[] size = new int[1];
        MPI.COMM_WORLD.Recv( size,0, 1, MPI.INT,0,0);
        while(size[0] != 0) {
            int[] array = new int[size[0]];
            MPI.COMM_WORLD.Recv( array,0, size[0], MPI.INT,0,0);
            this.queue.add(new Node(array));
            MPI.COMM_WORLD.Recv( size,0, 1, MPI.INT,0,0);
        }
    }

    public void findClosedConcepts() {
        Integer rank = MPI.COMM_WORLD.Rank();
        totalConcepts = 0;

        while(!queue.isEmpty()) {
            Node node = queue.remove();
            T.get(node.marker - 1).clear();
            RoaringBitmap usefulObjectList = findCommonObjects(node.seedConcept);
            for(Integer objectID: usefulObjectList) {
                T.get(node.marker - 1).add(objectID);
            }
            findClosedConcepts(node.seedConcept, node.marker);
        }

        System.out.println("Machine " + rank + " End Time: " + (System.nanoTime() - startTime )/1000000000.0);

        if(rank != 0) {
            int numberOfConcepts[] = new int[1];
            numberOfConcepts[0] = totalConcepts;
            MPI.COMM_WORLD.Send( numberOfConcepts, 0, 1, MPI.INT, 0, 0);
        }
        else {
            int numberOfConcepts[] = new int[1];
            for (Integer i = 1; i < MPI.COMM_WORLD.Size(); i++) {
                MPI.COMM_WORLD.Recv( numberOfConcepts, 0, 1, MPI.INT, i, 0);
                totalConcepts += numberOfConcepts[0];
            }
        }
    }

    public void occurrenceDeliver(Integer marker) {

        for(Integer i = marker; i <= data.totalAttributes; i++) {
            T.get(i).clear();
        }

        for(Integer objectID: T.get(marker - 1)) {
            for(Integer attributeID: data.objectMap.get(objectID).attributeList) {
                if(attributeID >= marker) {
                    T.get(attributeID).add(objectID);
                }
            }
        }
    }

    public void findClosedConcepts(RoaringBitmap seedConcept, Integer marker) {

        totalConcepts++;
        if(marker > data.totalAttributes) {
            return;
        }
        occurrenceDeliver(marker);

        for(int i = data.totalAttributes; i >= marker; i--){
            if(!seedConcept.contains(i)) {

                RoaringBitmap childConcept = findCommonAttributes(T.get(i));

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
                    findClosedConcepts(childConcept, i + 1);
                }
            }
        }
    }
}