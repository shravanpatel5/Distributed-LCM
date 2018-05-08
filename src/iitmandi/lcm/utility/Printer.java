package iitmandi.lcm.utility;

import iitmandi.lcm.model.*;
import java.util.ArrayList;

public class Printer {
    public static void printData(Data data) {
        System.out.println("Number of Objects: " + data.totalObjects);
        System.out.println("Number of Attributes: " + data.totalAttributes);
        System.out.println("Object Map:");
        for(Integer i = 1; i <= data.totalObjects; i++) {
            System.out.print("Object-" + i + ": ");
            for(Integer x: data.objectMap.get(i).attributeList) {
                System.out.print(x + " ");
            }
            System.out.println("");
        }
        System.out.println("Attribute Map:");
        for(Integer i = 1; i <= data.totalAttributes; i++) {
            System.out.print("Attribute-" + i + ": ");
            for(Integer x: data.attributeMap.get(i).objectList) {
                System.out.print(x + " ");
            }
            System.out.println("");
        }
    }
}