package iitmandi.lcm;

import iitmandi.lcm.model.*;
import iitmandi.lcm.utility.*;
import mpi.*;

public class Main {
    public static void main(String[] args){
        String arguments[] = MPI.Init(args);
        String fileName;
        if(arguments.length == 0) {
//            fileName = "/home/shravan/Desktop/SmallMushroom.txt";
            System.out.println("Please enter filename");
            return;
        }
        else {
            fileName = arguments[0];
        }
        Integer rank = MPI.COMM_WORLD.Rank();
        Data data = new Data();
        Reader.readFile(fileName, data);
        long startTime = System.nanoTime();
        LCM.startTime = startTime;
        LCM lcm = new LCM(data);
        if(rank == 0) {
            RequestHandler requestHandler = new RequestHandler(MPI.COMM_WORLD.Size() - 1, 8);
            requestHandler.updateWork(1, data.totalAttributes, true);
            requestHandler.start();
            System.out.println("\nNumber of Concepts = " + requestHandler.totalConcepts);
            double endTime = System.nanoTime();
            System.out.println("Time Taken: "+ (endTime - startTime)/1000000000.0 + " seconds");
        }
        else {
            if(rank == 1) {
                lcm.insertInitialWork();
            }
            lcm.findClosedConcepts();
        }
        MPI.Finalize();
    }
}