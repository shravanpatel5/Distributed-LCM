package iitmandi.lcm;

import iitmandi.lcm.model.*;
import mpi.MPI;
import org.roaringbitmap.RoaringBitmap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.math.BigInteger;

public class LCM {

    private BigInteger workSize;
    private Integer workSizeInLog;
    private static BigInteger ZERO = new BigInteger("0");
    private static BigInteger ONE = new BigInteger("1");
    private static BigInteger TWO = new BigInteger("2");
    public int totalConcepts;
    public static double startTime;
    private Data data;
    private Deque<Node> deque;
    private ArrayList<ArrayList<Integer>> T;

    public LCM(Data data) {
        this.data = data;
        deque = new LinkedList<>();
        T = new ArrayList<>();
        for(Integer i = 0; i <= data.totalAttributes; i++) {
            T.add(new ArrayList<>());
        }
        workSizeInLog = -1;
        workSize = new BigInteger("0");
    }

    public void insertInitialWork() {
        RoaringBitmap rootConcept = findCommonAttributes(findCommonObjects(new RoaringBitmap()));
        Node root = new Node(rootConcept,1);
        deque.clear();
        deque.addFirst(root);
        for(Integer i = 1; i <= data.totalObjects; i++) {
            T.get(0).add(i);
        }
        workSize = TWO.pow(data.totalAttributes);
        updateWorkSizeInLog();
    }

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

    private Node receiveWork(Integer sourceID) {
        int[] size = new int[1];
        MPI.COMM_WORLD.Recv( size,0, 1, MPI.INT,sourceID,0);
//        System.out.println("M"+ MPI.COMM_WORLD.Rank() + " Work received from " + sourceID);

        if(size[0] == 0) {
            return null;
        }
        int[] array = new int[size[0]];
        MPI.COMM_WORLD.Recv( array,0, size[0], MPI.INT,sourceID,0);
        return new Node(array);
    }

    private void giveWork(Integer requesterID) {
        int[] size = new int[1];
        if(!deque.isEmpty()) {
            Node node = deque.removeLast();
            workSize = workSize.subtract(TWO.pow(data.totalAttributes - node.marker + 1));
            int[] array = node.toArray();
            size[0] = array.length;
            MPI.COMM_WORLD.Send( size, 0, 1, MPI.INT, requesterID, 0);
            MPI.COMM_WORLD.Send( array, 0, array.length, MPI.INT, requesterID, 0);
        }
        else {
            size[0] = 0;
            MPI.COMM_WORLD.Send( size, 0, 1, MPI.INT, requesterID, 0);
        }
    }

    private Boolean updateWorkSizeInLog() {
        Integer logSize = (int) Math.ceil( Math.log(workSize.doubleValue()) / Math.log(2) );
        if (workSize.compareTo(ZERO) == 0) {
            logSize = -1;
        }
        if (logSize != workSizeInLog) {
            workSizeInLog = logSize;
            return true;
        }
        return false;
    }

    public void findClosedConcepts() {
        totalConcepts = 0;
        int request[] = new int[1];

        while (true) {
            while (!deque.isEmpty()) {
                Node node = deque.removeFirst();
                workSize = workSize.subtract(TWO.pow(data.totalAttributes - node.marker + 1));
                insertChildren(node.seedConcept, node.marker);
                if(updateWorkSizeInLog() == true) {
                    request[0] = workSizeInLog;
                    MPI.COMM_WORLD.Isend( request, 0, 1, MPI.INT, 0, 0);
                }
                MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(MPI.ANY_SOURCE,0);
                if(MPI.EMPTY_STATUS != null) {
                    MPI.COMM_WORLD.Recv( request,0, 1, MPI.INT,MPI.EMPTY_STATUS.source,0);
                    //System.out.println("M"+ MPI.COMM_WORLD.Rank() + " Work Request Received from " + MPI.EMPTY_STATUS.source);
                    giveWork(MPI.EMPTY_STATUS.source);
                    updateWorkSizeInLog();
                    request[0] = workSizeInLog;
//                    System.out.println("M"+ MPI.COMM_WORLD.Rank() + " " + request[0]);
                    MPI.COMM_WORLD.Isend( request, 0, 1, MPI.INT, 0, 2);
                }
            }
            //System.out.println("M"+ MPI.COMM_WORLD.Rank() + " Emptied");
            MPI.COMM_WORLD.Isend( request, 0, 1, MPI.INT, 0, 1);
            MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(0,0);
            while(MPI.EMPTY_STATUS == null) {
                MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(MPI.ANY_SOURCE,0);
                if(MPI.EMPTY_STATUS != null) {
                    if(MPI.EMPTY_STATUS.source != 0) {
                        MPI.COMM_WORLD.Recv( request,0, 1, MPI.INT,MPI.EMPTY_STATUS.source,0);
//                        System.out.println("M"+ MPI.COMM_WORLD.Rank() + " Work Request Received from " + MPI.EMPTY_STATUS.source);
                        giveWork(MPI.EMPTY_STATUS.source);
                        updateWorkSizeInLog();
                        request[0] = workSizeInLog;
                        MPI.COMM_WORLD.Isend( request, 0, 1, MPI.INT, 0, 2);

                        MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(0,0);
                    }
                    else {
                        break;
                    }
                }
            }
            MPI.COMM_WORLD.Recv( request,0, 1, MPI.INT,0,0);
            if(request[0] == -1) {
                continue;
            }
            else if(request[0] == 0) {
                break;
            }
            else {
//                System.out.println("Requested work to " + request[0]);
                MPI.COMM_WORLD.Send( request, 0, 1, MPI.INT, request[0], 0);
                Node node = receiveWork(request[0]);
                if(node != null) {
                    deque.addFirst(node);
                    workSize = workSize.add(TWO.pow( data.totalAttributes - node.marker + 1));
                    updateWorkSizeInLog();
                    request[0] = workSizeInLog;
//                    System.out.println("M"+ MPI.COMM_WORLD.Rank() + " " + request[0]);
                    MPI.COMM_WORLD.Isend( request, 0, 1, MPI.INT, 0, 2);
                    T.get(node.marker - 1).clear();
                    RoaringBitmap usefulObjectList = findCommonObjects(node.seedConcept);
                    for (Integer objectID : usefulObjectList) {
                        T.get(node.marker - 1).add(objectID);
                    }
                }
                else {
                    request[0] = workSizeInLog;
                    MPI.COMM_WORLD.Isend( request, 0, 1, MPI.INT, 0, 2);
                }
            }
        }

        System.out.println("Machine " + MPI.COMM_WORLD.Rank() + " End Time: " + (System.nanoTime() - startTime )/1000000000.0);

        int numberOfConcepts[] = new int[1];
        numberOfConcepts[0] = totalConcepts;
        MPI.COMM_WORLD.Send( numberOfConcepts, 0, 1, MPI.INT, 0, 0);
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

    public void insertChildren(RoaringBitmap seedConcept, Integer marker) {
        totalConcepts++;

        if(marker > data.totalAttributes) {
            return;
        }

        occurrenceDeliver(marker);

        for(int i = marker; i <= data.totalAttributes; i++) {
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
                    Node node = new Node(childConcept, i+1);
                    deque.addFirst(node);
                    workSize = workSize.add(TWO.pow( data.totalAttributes - i));
                }
            }
        }
    }
}