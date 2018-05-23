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
        updateWorkSizeInLog(false);
    }

    public void findClosedConcepts() {
        totalConcepts = 0;
        int buffer[] = new int[1];

        while (true) {
            while (!deque.isEmpty()) {
                exploreTreeNode();
                checkAndGiveWork(); // Giving work if any request has come
            }
            //System.out.println("M"+ MPI.COMM_WORLD.Rank() + " Emptied");

            // Asking Giver's ID
            send(0, 0, 1);

            // Receiving Giver's ID
            MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(0,0);
            while(MPI.EMPTY_STATUS == null) {
                checkAndGiveWork();
                MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(0,0);
            }
            MPI.COMM_WORLD.Recv( buffer,0, 1, MPI.INT,0,0);
            Integer giverID = buffer[0];

            if(giverID == -1) {
                continue;
            }
            else if(giverID == 0) {
                //  This is Termination Signal
                break;
            }
            else {
//                System.out.println("Requested work to " + buffer[0]);
                send(0, giverID, 0);
                Node node = receiveWork(giverID);
                if(node != null) {
                    addNodeInDeque(node);
                }
                else {
                    send(workSizeInLog, 0, 2);
                }
            }
        }

        System.out.println("Machine " + MPI.COMM_WORLD.Rank() + " End Time: " + (System.nanoTime() - startTime )/1000000000.0);

        // Sending total concepts to Machine 0
        send(totalConcepts, 0, 0);
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

    private void giveWork(Integer destination) {
        int[] size = new int[1];
        if(!deque.isEmpty()) {
            Node node = deque.removeLast();
            workSize = workSize.subtract(TWO.pow(data.totalAttributes - node.marker + 1));
            int[] buffer = node.toArray();
            size[0] = buffer.length;
            MPI.COMM_WORLD.Send( size, 0, 1, MPI.INT, destination, 0);
            MPI.COMM_WORLD.Send( buffer, 0, buffer.length, MPI.INT, destination, 0);
        }
        else {
            size[0] = 0;
            MPI.COMM_WORLD.Send( size, 0, 1, MPI.INT, destination, 0);
        }
    }

    private void updateWorkSizeInLog(Boolean updateGlobally) {
        Integer logSize = (int) Math.ceil( Math.log(workSize.doubleValue()) / Math.log(2) );
        if (workSize.compareTo(ZERO) == 0) {
            logSize = -1;
        }
        if (logSize != workSizeInLog) {
            workSizeInLog = logSize;
            if(updateGlobally) {
                send(workSizeInLog, 0, 0);
            }
        }
    }

    void exploreTreeNode() {
        Node node = deque.removeFirst();
        workSize = workSize.subtract(TWO.pow(data.totalAttributes - node.marker + 1));
        insertChildren(node.seedConcept, node.marker);
        updateWorkSizeInLog(true);
    }

    void send(int value, int destination, int tag) {
        int buffer[] = new int[1];
        buffer[0] = value;
        MPI.COMM_WORLD.Isend( buffer, 0, 1, MPI.INT, destination, tag);
    }

    private void checkAndGiveWork() {
        int buffer[] = new int[1];
        MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(MPI.ANY_SOURCE,0);
        if(MPI.EMPTY_STATUS != null) {
            if(MPI.EMPTY_STATUS.source != 0) {
                MPI.COMM_WORLD.Recv( buffer,0, 1, MPI.INT,MPI.EMPTY_STATUS.source,0);
//                System.out.println("M"+ MPI.COMM_WORLD.Rank() + " Work Request Received from " + MPI.EMPTY_STATUS.source);
                giveWork(MPI.EMPTY_STATUS.source);
                updateWorkSizeInLog(false);
                send(workSizeInLog, 0, 2);
            }
        }
    }

    private void addNodeInDeque(Node node) {
        deque.addFirst(node);
        workSize = workSize.add(TWO.pow( data.totalAttributes - node.marker + 1));
        updateWorkSizeInLog(false);
        send(workSizeInLog, 0, 2);

        // Initializing T for new node
        T.get(node.marker - 1).clear();
        RoaringBitmap usefulObjectList = findCommonObjects(node.seedConcept);
        for (Integer objectID : usefulObjectList) {
            T.get(node.marker - 1).add(objectID);
        }
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
}