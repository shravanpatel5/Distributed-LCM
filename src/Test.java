import java.util.ArrayList;

public class Test {
    public static void main(String[] args){
        Data data = new Data();
        Reader.readFile("/home/shravan/IdeaProjects/LCM_V1.0/src/Data.txt", data);
        //Printer.printData(data);
        LCM lcm = new LCM();
	long startTime = System.nanoTime();
        lcm.findClosedConcepts(data, new ArrayList<Integer>(), 1);
	long endTime = System.nanoTime();
        System.out.println("Number of Concepts = " + lcm.totalConcepts);
	System.out.println("Time Taken: "+ (endTime - startTime)/1000000000.0 + " seconds");
    }
}
