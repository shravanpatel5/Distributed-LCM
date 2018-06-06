package iitmandi.lcm;

import iitmandi.lcm.model.*;
import iitmandi.lcm.utility.*;
import mpi.*;

public class Main {
    public static void main(String[] args){
        String arguments[] = MPI.Init(args);
        String fileName = "/home/shravan/Downloads/SmallMushroom.txt";
        Integer minWorkThreshold = 8;
        Boolean giveLastWork = true;
        Boolean blocking = true;
        Boolean startWithStaticDistribution = false;
        if(arguments.length == 0) {
            System.out.println("Usage: filename termination-threshold give-second-last-work non-blocking start-with-static-distribution");
            return;
        }
        if(arguments.length >= 1) {
            fileName = arguments[0];
        }
        if(arguments.length >= 2) {
            minWorkThreshold = Integer.parseInt(arguments[1]);
        }
        if(arguments.length >= 3) {
            if( Integer.parseInt(arguments[2]) == 1) {
                giveLastWork = false;
            }
        }
        if(arguments.length >= 4) {
            if( Integer.parseInt(arguments[3]) == 1) {
                blocking = false;
            }
        }
        if(arguments.length >= 5) {
            if( Integer.parseInt(arguments[4]) == 1) {
                startWithStaticDistribution = true;
            }
        }
        Integer rank = MPI.COMM_WORLD.Rank();
        Data data = new Data();
        Reader.readFile(fileName, data);
        LCM lcm = new LCM(data);
        long startTime = System.nanoTime();
        lcm.startTime = startTime;
        lcm.giveLastWork = giveLastWork;
        if(rank == 0) {
            if(startWithStaticDistribution) {
                lcm.distributeWorkInitially(MPI.COMM_WORLD.Size() - 1);
            }
            RequestHandler requestHandler = new RequestHandler(MPI.COMM_WORLD.Size() - 1);
            requestHandler.minWorkThreshold = minWorkThreshold;
            requestHandler.blocking = blocking;
            requestHandler.startWithStaticDistribution = startWithStaticDistribution;
            if(!startWithStaticDistribution) {
                requestHandler.updateWork(1, data.totalAttributes);
            }
            requestHandler.start();
            System.out.println("--------------------------------------");
            System.out.println("Number of Concepts = " + requestHandler.totalConcepts);
            System.out.println("Number of Work Transfer: "+ requestHandler.countOfWorkRequest);
            double endTime = System.nanoTime();
            System.out.println("Time Taken: "+ (endTime - startTime)/1000000000.0 + " seconds");
            System.out.println("--------------------------------------");
        }
        else {
            if(startWithStaticDistribution) {
                lcm.receiveWorkInitially();
            }
            else {
                if (rank == 1) {
                    lcm.insertInitialWork();
                }
            }
            lcm.findClosedConcepts();
        }
        MPI.Finalize();
    }
}