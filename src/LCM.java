import java.util.*;

public class LCM {
    public int totalConcepts;

    public LCM() {
        totalConcepts = 0;
    }

    private ArrayList<Integer> findCommonObjects(Data data, ArrayList<Integer> attributes){
        Map<Integer,Integer> map = new HashMap<Integer, Integer>();
        for(int i=1; i<=data.totalObjects; i++){
            map.put(i,1);
        }
        for(int attribute: attributes){
            for(int object: data.objects.get(attribute)){
                map.replace(object,map.get(object)+1);
            }
        }
        ArrayList<Integer> commonObjects = new ArrayList<Integer>();
        for(Map.Entry< Integer,Integer> pair: map.entrySet()){
            if(pair.getValue()==attributes.size()+1){
                commonObjects.add(pair.getKey());
            }
        }
        return commonObjects;
    }

    private ArrayList<Integer> findCommonAttributes(Data data, ArrayList<Integer> objects){
        Map<Integer,Integer> map = new HashMap<Integer, Integer>();
        for(int i=1; i<=data.totalAttributes; i++){
            map.put(i,1);
        }
        for(int object: objects){
            for(int attribute: data.attributes.get(object)){
                    map.replace(attribute,map.get(attribute)+1);
            }
        }
        ArrayList<Integer> commonAttributes = new ArrayList<Integer>();
        for(Map.Entry< Integer,Integer> pair: map.entrySet()){
            if(pair.getValue()==objects.size()+1){
                commonAttributes.add(pair.getKey());
            }
        }
        return commonAttributes;
    }

    public void findClosedConcepts(Data data, ArrayList<Integer> seedConcept, int marker) {
        if(seedConcept.size()==0) {
            ArrayList<Integer> extent = findCommonObjects(data, seedConcept);
            seedConcept = findCommonAttributes(data, extent);
        }
//        System.out.println("Marker = " + marker);
        //Printer.printConcept(seedConcept);
        totalConcepts++;
        //System.out.println(totalConcepts);
        if(marker > data.totalAttributes) {
            return;
        }
        for(int i=marker; i<=data.totalAttributes; i++){
            if(!seedConcept.contains(i)) {
                seedConcept.add(i);
                ArrayList<Integer> childConcept = findCommonAttributes(data, findCommonObjects(data, seedConcept));
                seedConcept.remove(seedConcept.size() - 1);

                Set<Integer> attributes = new HashSet<Integer>();
                for(int attribute: childConcept){
                    if(attribute < i){
                        attributes.add(attribute);
                    }
                }
                for(int attribute: seedConcept){
                    if(attribute < i){
                        attributes.remove(attribute);
                    }
                }
                if(attributes.size()==0) {
//                    System.out.println("i: " + i);
//                    System.out.println("Child:");
//                    Printer.printArrayList(childConcept);
                    findClosedConcepts(data, childConcept, i + 1);
                }
            }
        }
    }
}