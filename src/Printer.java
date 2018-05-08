import java.util.ArrayList;

public class Printer {
    public static void printData(Data data){
        System.out.println("Objects: " + data.totalObjects);
        System.out.println("Attributes: " + data.totalAttributes);
        System.out.println("Attributes List");
        for(int i=1; i<=data.totalObjects; i++){
            System.out.print(i + ": ");
            for(Integer x: data.attributes.get(i)){
                System.out.print(x + " ");
            }
            System.out.println("");
        }
        System.out.println("Objects List");
        for(int i=1; i<=data.totalAttributes; i++){
            System.out.print(i + ": ");
            for(Integer x: data.objects.get(i)){
                System.out.print(x + " ");
            }
            System.out.println("");
        }
    }
    public static void printConcept(ArrayList<Integer> attributes, ArrayList<Integer> objects){
        System.out.print("Attributes: ");
        for(int attribute: attributes){
            System.out.print(attribute + " ");
        }
        System.out.println("");
        System.out.print("Objects: ");
        for(int object: objects){
            System.out.print(object + " ");
        }
        System.out.println("");
    }
    public static void printConcept(ArrayList<Integer> attributes){
        System.out.print("Attributes: ");
        for(int attribute: attributes){
            System.out.print(attribute + " ");
        }
        System.out.println("");
    }
    public static void printArrayList(ArrayList<Integer> arrayList){
        for(int x: arrayList){
            System.out.print(x + " ");
        }
        System.out.println("");
    }
}
