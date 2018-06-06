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
    public Integer minWorkThreshold;
    public Integer countOfWorkRequest;
    public Boolean blocking;
    public Boolean startWithStaticDistribution;

    public RequestHandler(Integer numberOfRequesters) {
        totalConcepts = 0;
        this.numberOfRequesters = numberOfRequesters;
        workTree = new TreeSet<Pair>(new PairComparator());
        workList = new ArrayList<>();
        workList.add(0);
        for(Integer i = 1; i <= numberOfRequesters; i++) {
            workTree.add(new Pair(i, -1));
            workList.add(-1);
        }
        countOfWorkRequest = 0;
    }

    public void updateWork(Integer ID, Integer work) {
        workTree.remove(new Pair(ID, workList.get(ID)));
        workList.set(ID, work);
        workTree.add(new Pair(ID, work));
    }

    private Integer maxWork() {
        return workTree.first().work;
    }

    private Integer IDWithMaxWork() {
        return workTree.first().ID;
    }

    public void start() {
        Integer countOfTerminationSignal = 0;
        int buffer[] = new int[1];
        Integer numberOfTerminatedRequesters = 0;
        Boolean isTerminated = false;
        if (numberOfRequesters == 1) {
            isTerminated = true;
        }
        if(startWithStaticDistribution) {
            for (Integer i = 1; i <= numberOfRequesters; i++) {
                MPI.EMPTY_STATUS = MPI.COMM_WORLD.Recv( buffer, 0, 1, MPI.INT, MPI.ANY_SOURCE, 4);
                Integer requesterID = MPI.EMPTY_STATUS.source;
                if(!isTerminated) {
                    updateWork(requesterID, buffer[0]);
                }
            }
        }
        while (true) {
            MPI.EMPTY_STATUS = MPI.COMM_WORLD.Recv( buffer, 0, 1, MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            Integer requesterID = MPI.EMPTY_STATUS.source;
            if (MPI.EMPTY_STATUS.tag == 0 || MPI.EMPTY_STATUS.tag == 2) {
                if (!isTerminated) {
                    updateWork(requesterID, buffer[0]);
                }
            }
            else if(MPI.EMPTY_STATUS.tag == 1) {
                if(isTerminated == true) {
                    countOfTerminationSignal++;
                    buffer[0] = 0;
                    MPI.COMM_WORLD.Send( buffer, 0, 1, MPI.INT, requesterID, 0);
                }
                else {
                    Integer giverID = IDWithMaxWork();
                    if(giverID == requesterID) {
                        buffer[0] = -1;
                        MPI.COMM_WORLD.Send( buffer, 0, 1, MPI.INT, requesterID, 0);
                    }
                    else {
                        countOfWorkRequest++;
                        buffer[0] = giverID;
                        MPI.COMM_WORLD.Send(buffer, 0, 1, MPI.INT, requesterID, 0);
                        if(!blocking) {
                            Integer estimatedDonatedWork = workList.get(giverID) - 1;
                            updateWork(giverID, estimatedDonatedWork - 1);
                            updateWork(requesterID, estimatedDonatedWork );
                        }
                        else {
                            MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, giverID, 2);
                            updateWork(giverID, buffer[0]);
                            MPI.COMM_WORLD.Recv(buffer, 0, 1, MPI.INT, requesterID, 2);
                            updateWork(requesterID, buffer[0]);
                        }
                    }
                }
            }
            else {
                totalConcepts += buffer[0];
                numberOfTerminatedRequesters++;
            }
            if(!isTerminated && maxWork() < minWorkThreshold) {
                isTerminated = true;
            }
            if(countOfTerminationSignal == numberOfRequesters) {
                break;
            }
        }
        for (Integer i = 1; i <= numberOfRequesters - numberOfTerminatedRequesters; i++) {
            MPI.COMM_WORLD.Recv( buffer, 0, 1, MPI.INT, MPI.ANY_SOURCE, 3);
            totalConcepts += buffer[0];
        }
    }
}