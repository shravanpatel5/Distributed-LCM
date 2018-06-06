package iitmandi.lcm;

import iitmandi.lcm.model.*;
import mpi.MPI;
import org.roaringbitmap.RoaringBitmap;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.math.BigInteger;
import java.util.Queue;

public class LCM {

    private BigInteger workSize;
    private Integer workSizeInLog;
    private BigInteger ZERO = new BigInteger("0");
    private BigInteger TWO = new BigInteger("2");
    public int totalConcepts;
    public double startTime;
    private Data data;
    private Deque<Node> deque;
    private Deque<Node> initialDeque;
    private ArrayList<ArrayList<Integer>> T;
    private Integer countOfWorkRequest;
    private long idleTime;
    private long workTransferTime;
    public Boolean giveLastWork;

    public LCM(Data data) {
        this.data = data;
        deque = new LinkedList<>();
        initialDeque = new LinkedList<>();
        T = new ArrayList<>();
        for(Integer i = 0; i <= data.totalAttributes; i++) {
            T.add(new ArrayList<>());
        }
        workSizeInLog = -1;
        workSize = new BigInteger("0");
        countOfWorkRequest = 0;
        idleTime = 0;
        workTransferTime = 0;
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

            if(!initialDeque.isEmpty()) {
                addNodeInDeque(initialDeque.removeFirst());
                continue;
            }

            // Asking Giver's ID
            send(0, 0, 1);

            long startTime = System.nanoTime();

            // Receiving Giver's ID
            MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(0,0);
            while(MPI.EMPTY_STATUS == null) {
                checkAndGiveWork();
                MPI.EMPTY_STATUS = MPI.COMM_WORLD.Iprobe(0,0);
            }

            long endTime = System.nanoTime();
            idleTime += endTime - startTime;

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
                countOfWorkRequest++;
                send(0, giverID, 0);
                Node node = receiveWork(giverID);
                if(node != null) {
                    addNodeInDeque(node);
                    workSize = workSize.add(TWO.pow( data.totalAttributes - node.marker + 1));
                    updateWorkSizeInLog(false);
                }
                send(workSizeInLog, 0, 2);
            }
        }

        System.out.println("--------------------------------------\nMachine " + MPI.COMM_WORLD.Rank() + ":\nEnd Time: " + (System.nanoTime() - startTime )/1000000000.0 + "\nCount of requesting work: " + countOfWorkRequest + "\nIdle Time: " + idleTime/1000000000.0 + "\nWork Transfer Time: " + workTransferTime/1000000000.0);

        // Sending total concepts to Machine 0
        send(totalConcepts, 0, 3);
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
        if(size[0] == 0) {
            return null;
        }
        int[] array = new int[size[0]];
        long startTime = System.nanoTime();
        MPI.COMM_WORLD.Recv( array,0, size[0], MPI.INT,sourceID,0);
        long endTime = System.nanoTime();
        workTransferTime += endTime - startTime;
        return new Node(array);
    }

    private void giveSecondLastWork(Integer destination) {
        Node node;
        if(initialDeque.size() > 1) {
            Node tempNode = initialDeque.removeLast();
            node = initialDeque.removeLast();
            initialDeque.addLast(tempNode);
        }
        else if(initialDeque.size() == 1) {
            node = initialDeque.removeLast();
        }
        else if(deque.size() > 1) {
            Node lastNode = deque.removeLast();
            node = deque.removeLast();
            deque.addLast(lastNode);
        }
        else {
            send(0, destination, 0);
            return;
        }
        workSize = workSize.subtract(TWO.pow(data.totalAttributes - node.marker + 1));
        int[] buffer = node.toArray();
        long startTime = System.nanoTime();
        send(buffer.length, destination, 0);
        MPI.COMM_WORLD.Send( buffer, 0, buffer.length, MPI.INT, destination, 0);
        long endTime = System.nanoTime();
        workTransferTime += endTime - startTime;
    }

    private void giveLastWork(Integer destination) {
        Node node;
        if(!initialDeque.isEmpty()) {
            node = initialDeque.removeLast();
        }
        else if(deque.size() > 1) {
            node = deque.removeLast();
        }
        else {
            send(0, destination, 0);
            return;
        }
        workSize = workSize.subtract(TWO.pow(data.totalAttributes - node.marker + 1));
        int[] buffer = node.toArray();
        long startTime = System.nanoTime();
        send(buffer.length, destination, 0);
        MPI.COMM_WORLD.Send( buffer, 0, buffer.length, MPI.INT, destination, 0);
        long endTime = System.nanoTime();
        workTransferTime += endTime - startTime;
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

    void send(Integer value, Integer destination, Integer tag) {
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
                if(giveLastWork == true) {
                    giveLastWork(MPI.EMPTY_STATUS.source);
                }
                else {
                    giveSecondLastWork(MPI.EMPTY_STATUS.source);
                }
                updateWorkSizeInLog(false);
                send(workSizeInLog, 0, 2);
            }
        }
    }

    private void addNodeInDeque(Node node) {
        deque.addFirst(node);

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

    public void distributeWorkInitially(Integer numberOfWorkers) {
        System.out.println("--------------------------------------");
        System.out.println("Initial Distribution Started");
        Queue<Node> queue = new LinkedList<>();
        RoaringBitmap rootConcept = findCommonAttributes(findCommonObjects(new RoaringBitmap()));
        Node root = new Node(rootConcept, 1);
        queue.add(root);
        int[] size = new int[1];

        while (!queue.isEmpty() && (data.totalAttributes - queue.peek().marker) >= 1) {
            Integer sizeOfQueue = queue.size();
            for (Integer i = 0; i < sizeOfQueue; i++) {
                Node node = queue.remove();
                Node child = generateChild(node);
                node.marker++;
                queue.add(node);
                if (child != null) {
                    queue.add(child);
                }
            }
            if (queue.size() >= numberOfWorkers) {
                for (Integer i = 1; i <= numberOfWorkers; i++) {
                    Node node = queue.remove();
                    int[] array = node.toArray();
                    send(array.length, i, 0);
                    MPI.COMM_WORLD.Send(array, 0, array.length, MPI.INT, i, 0);
                }
            }
        }
        for (Integer i = 2; i <= numberOfWorkers; i++) {
            send(0, i, 0);
        }
        while(!queue.isEmpty()) {
            Node node = queue.remove();
            int[] array = node.toArray();
            send(array.length, 1, 0);
            MPI.COMM_WORLD.Send(array, 0, array.length, MPI.INT, 1, 0);
        }
        send(0, 1, 0);
        System.out.println("Distribution Completed: " + (System.nanoTime() - startTime )/1000000000.0);
    }

    public void receiveWorkInitially() {
        int[] size = new int[1];
        MPI.COMM_WORLD.Recv( size,0, 1, MPI.INT,0,0);
        while(size[0] != 0) {
            int[] array = new int[size[0]];
            MPI.COMM_WORLD.Recv( array,0, size[0], MPI.INT,0,0);
            Node node = new Node(array);
            this.initialDeque.add(node);
            workSize = workSize.add(TWO.pow( data.totalAttributes - node.marker + 1));
            updateWorkSizeInLog(false);
            MPI.COMM_WORLD.Recv( size,0, 1, MPI.INT,0,0);
        }
        send(workSizeInLog, 0, 4);
    }
}