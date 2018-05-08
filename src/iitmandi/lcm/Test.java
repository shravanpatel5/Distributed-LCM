package iitmandi.lcm;

import iitmandi.lcm.model.*;
import iitmandi.lcm.utility.*;
import org.roaringbitmap.RoaringBitmap;

public class Test {
    public static void main(String[] args){
        Data data = new Data();
        Reader.readFile("/home/shravan/IdeaProjects/LCM/src/iitmandi/lcm/SmallMushroom.txt", data);
//        Printer.printData(data);
        LCM0 lcm = new LCM0();
        long startTime = System.nanoTime();
        lcm.findClosedConcepts(data, new RoaringBitmap(), 1);
        double endTime = System.nanoTime();
        System.out.println("Number of Concepts = " + lcm.totalConcepts);
        System.out.println("Time Taken: "+ (endTime - startTime)/1000000000.0 + " seconds");
    }
}