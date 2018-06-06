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
        if(arguments.length == 0) {
            System.out.println("Usage: filename termination-threshold give-second-last-work non-blocking");
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
            if( Integer.parseInt(arguments[2]) == 1) {
                blocking = false;
            }
        }
        Integer rank = MPI.COMM_WORLD.Rank();
        Data data = new Data();
        Reader.readFile(fileName, data);
        long startTime = System.nanoTime();
        if(rank == 0) {
            RequestHandler requestHandler = new RequestHandler(MPI.COMM_WORLD.Size() - 1, minWorkThreshold);
            requestHandler.blocking = blocking;
            requestHandler.updateWork(1, data.totalAttributes);
            requestHandler.start();
            System.out.println("\n\nNumber of Concepts = " + requestHandler.totalConcepts);
            System.out.println("Number of Work Transfer: "+ requestHandler.countOfWorkRequest);
            double endTime = System.nanoTime();
            System.out.println("Time Taken: "+ (endTime - startTime)/1000000000.0 + " seconds");
        }
        else {
            LCM lcm = new LCM(data);
            lcm.startTime = startTime;
            lcm.giveLastWork = giveLastWork;
            if(rank == 1) {
                lcm.insertInitialWork();
            }
            lcm.findClosedConcepts();
        }
        MPI.Finalize();
    }
}