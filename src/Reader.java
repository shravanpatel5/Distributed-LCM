import java.io.*;
import java.util.ArrayList;

public class Reader {
    public static void readFile(String fileName, Data data){
        File file = new File(fileName);
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            int totalAttributes = 0;
            data.attributes.add(new ArrayList<Integer>());

            while ((line = bufferedReader.readLine()) != null) {
                String[] stringArray = line.split(" ");
                ArrayList<Integer> arrayList = new ArrayList<Integer>();
                for (int i = 0; i < stringArray.length; i++) {
                    int attribute = Integer.parseInt(stringArray[i]);
                    arrayList.add(attribute);
                    totalAttributes = Math.max(attribute,totalAttributes);
                }
                data.attributes.add(arrayList);
            }
            data.totalAttributes = totalAttributes;
            data.totalObjects = data.attributes.size() - 1;
            for(int i=0 ; i<=data.totalAttributes; i++){
                data.objects.add(new ArrayList<Integer>());
            }
            for(int i=1; i<=data.totalObjects; i++){
                ArrayList<Integer> arrayList = data.attributes.get(i);
                for(Integer x: data.attributes.get(i)){
                    data.objects.get(x).add(i);
                }
            }
            fileReader.close();
            bufferedReader.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
