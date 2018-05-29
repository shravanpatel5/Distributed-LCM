package iitmandi.lcm;

import java.util.*;
import mpi.MPI;

class Pair {
    public Integer ID;
    public Integer work;
    public Pair(Integer ID, Integer work){
        this.ID = ID;
        this.work = work;
    }
}

class PairComparator implements Comparator<Pair> {
    @Override
    public int compare(Pair p1, Pair p2) {
        if(p1.work > p2.work) {
            return -1;
        }
        else if (p1.work < p2.work){
            return 1;
        }
        else if(p1.ID > p2.ID) {
            return -1;
        }
        else if (p1.ID < p2.ID){
            return 1;
        }
        else {
            return 0;
        }
    }
}

public class RequestHandler {

    private Integer numberOfRequesters;
    public Integer totalConcepts;
    private ArrayList<Integer> workList;
    private TreeSet<Pair> workTree;
    private Integer minWorkThreshold;
    public Integer countOfWorkRequest;

    public RequestHandler(Integer numberOfRequesters, Integer minWorkThreshold) {
        this.totalConcepts = 0;
        this.numberOfRequesters = numberOfRequesters;
        this.minWorkThreshold = minWorkThreshold;
        workTree = new TreeSet<Pair>(new PairComparator());
        workList = new ArrayList<>();
        workList.add(0);
        for(Integer i = 1; i <= numberOfRequesters; i++) {
            workTree.add(new Pair(i, -1));
            workList.add(-1);
        }
        this.countOfWorkRequest = 0;
    }

    private void printTree() {
        for(Pair p: workTree) {
            System.out.print("[" + p.ID + "," + p.work + "] ");
        }
        System.out.println();
    }

    public void updateWork(Integer ID, Integer work) {
//        if(workList.get(ID) > work) {
            workTree.remove(new Pair(ID, workList.get(ID)));
            workList.set(ID, work);
            workTree.add(new Pair(ID, work));
//        }
    }

    private Integer maxWork() {
        return workTree.first().work;
    }

    private Integer IDWithMaxWork() {
        return workTree.first().ID;
    }

    public void start() {
        Boolean isTerminated = false;
        if(numberOfRequesters == 1) {
            isTerminated = true;
        }
        Integer countOfTerminationSignal = 0;
        int buffer[] = new int[1];
        Integer conceptReceived = 0;
        while(true) {
            MPI.EMPTY_STATUS = MPI.COMM_WORLD.Recv( buffer, 0, 1, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            Integer requesterID = MPI.EMPTY_STATUS.source;
//            System.out.println(MPI.EMPTY_STATUS.tag + " " + requesterID + " " + maxWork());
            if( MPI.EMPTY_STATUS.tag == 0) {
                //System.out.println("Update Request from " + requesterID + " New Work " + buffer[0]);
                if(!isTerminated) {
                    updateWork(requesterID, buffer[0]);
                }
            }
            else if(MPI.EMPTY_STATUS.tag == 1) {
//                System.out.println("Work Request from " + requesterID);
                if(isTerminated == true) {
//                    System.out.println("Sent Termination Signal to " + requesterID);
                    countOfTerminationSignal++;
                    buffer[0] = 0;
                    MPI.COMM_WORLD.Send( buffer, 0, 1, MPI.INT, requesterID, 0);
                }
                else {
                    Integer giverID = IDWithMaxWork();
//                    System.out.println("Found giverID " + giverID);
                    if(giverID == requesterID) {
                        buffer[0] = -1;
                        MPI.COMM_WORLD.Send( buffer, 0, 1, MPI.INT, requesterID, 0);
                    }
                    else {
                        countOfWorkRequest++;
                        buffer[0] = giverID;
                        MPI.COMM_WORLD.Send(buffer, 0, 1, MPI.INT, requesterID, 0);
                        //System.out.println("Sent giverID " + giverID);

                        Integer estimatedDonatedWork = workList.get(giverID) - 1;
                        updateWork(giverID, estimatedDonatedWork - 1);
                        updateWork(requesterID, estimatedDonatedWork );
                    }
                }
            }
            else {
                totalConcepts += buffer[0];
                conceptReceived++;
            }
            if(!isTerminated && maxWork() < minWorkThreshold) {
                isTerminated = true;
            }
            if(countOfTerminationSignal == numberOfRequesters) {
                break;
            }
        }
        for (Integer i = 1; i <= numberOfRequesters - conceptReceived; i++) {
            MPI.COMM_WORLD.Recv( buffer, 0, 1, MPI.INT, MPI.ANY_SOURCE, 3);
            totalConcepts += buffer[0];
        }
    }
}