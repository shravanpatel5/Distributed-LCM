package iitmandi.lcm.utility;

import iitmandi.lcm.model.*;
import iitmandi.lcm.model.Object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class Reader {
    public static void readFile(String fileName, Data data){
        File file = new File(fileName);
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int totalAttributes = 0;
            int objectCounter = 0;
            data.objectMap.add(new Object(objectCounter));
            while ((line = bufferedReader.readLine()) != null) {
                objectCounter++;
                String[] stringArray = line.split(" ");
                data.objectMap.add(new Object(objectCounter));
                for (int i = 0; i < stringArray.length; i++) {
                    int attributeID = Integer.parseInt(stringArray[i]);
                    data.objectMap.get(objectCounter).attributeList.add(attributeID);
                    totalAttributes = Math.max(attributeID,totalAttributes);
                }
            }
            data.totalAttributes = totalAttributes;
            data.totalObjects = objectCounter;

            for(int i=0; i <= totalAttributes; i++){
                data.attributeMap.add(new Attribute(i));
            }
            for(int i=1; i <= objectCounter; i++){
                for(Integer x: data.objectMap.get(i).attributeList){
                    data.attributeMap.get(x).objectList.add(i);
                }
            }
            fileReader.close();
            bufferedReader.close();
        }
        catch(Exception e){
            System.err.println("Problem in reading file: " + fileName);
            e.printStackTrace();
        }
    }
}