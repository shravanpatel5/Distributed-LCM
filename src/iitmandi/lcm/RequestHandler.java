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
    TreeSet<Pair> workTree;
    Integer minWorkThreshold;

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
    }

    private void printTree() {
        for(Pair p: workTree) {
            System.out.print("[" + p.ID + "," + p.work + "] ");
        }
        System.out.println();
    }

    public void updateWork(Integer ID, Integer work, Boolean forcefully) {
        if(forcefully == true || workList.get(ID) > work) {
            workTree.remove(new Pair(ID, workList.get(ID)));
            workList.set(ID, work);
            workTree.add(new Pair(ID, work));
        }
    }

    private Integer maxWork() {
        return workTree.first().work;
    }

    private Integer IDWithMaxWork() {
        return workTree.first().ID;
    }

    public void start() {
        Boolean isTerminated = false;
        Integer countOfTerminationSignal = 0;
        int request[] = new int[1];

        while(true) {
            MPI.EMPTY_STATUS = MPI.COMM_WORLD.Recv( request, 0, 1, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            Integer requesterID = MPI.EMPTY_STATUS.source;
            if( MPI.EMPTY_STATUS.tag == 0) {
                //System.out.println("Update Request from " + requesterID + " New Work " + request[0]);
                updateWork(requesterID, request[0], false);
            }
            else {
                //System.out.println("Work Request from " + requesterID);
                if(isTerminated == true) {
                    //System.out.println("Sent Termination Signal to " + requesterID);
                    countOfTerminationSignal++;
                    request[0] = 0;
                    MPI.COMM_WORLD.Send( request, 0, 1, MPI.INT, requesterID, 0);
                }
                else {
                    Integer giverID = IDWithMaxWork();
                    //System.out.println("Found giverID " + giverID);
                    if(giverID == requesterID) {
                        request[0] = -1;
                        MPI.COMM_WORLD.Send( request, 0, 1, MPI.INT, requesterID, 0);
                    }
                    else {
                        request[0] = giverID;
                        MPI.COMM_WORLD.Send(request, 0, 1, MPI.INT, requesterID, 0);
                        //System.out.println("Sent giverID " + giverID);
                        MPI.COMM_WORLD.Recv(request, 0, 1, MPI.INT, giverID, 2);
                        //System.out.println("Update Request(T2) from " + giverID + " New Work " + request[0]);
                        updateWork(giverID, request[0], true);
                        MPI.COMM_WORLD.Recv(request, 0, 1, MPI.INT, requesterID, 2);
                        //System.out.println("Update Request(T2) from " + requesterID + " New Work " + request[0]);
                        updateWork(requesterID, request[0], true);
                    }
                }
            }
            if(maxWork() < minWorkThreshold) {
                isTerminated = true;
            }
            if(countOfTerminationSignal == numberOfRequesters) {
                break;
            }
        }
        for (Integer i = 1; i <= numberOfRequesters; i++) {
            MPI.COMM_WORLD.Recv( request, 0, 1, MPI.INT, MPI.ANY_SOURCE, 0);
            totalConcepts += request[0];
        }
    }
}