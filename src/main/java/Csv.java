import java.util.*;
import java.io.*;

//This class reads .csv files into a Csv object storing all its values as a String[][] array.
public class Csv
{
    private String[][] values;
    public int[] dimensions;
    
    //I have to use multiple Scanners because for some reason you can't reset them after using nextLine().
    private Scanner readDimensions, readRows, readFile;
    
    public Csv(String filePath){
        //setting up scanners and values array
        try {
            File file = new File(filePath);
            readFile = new Scanner(file);
            readRows = new Scanner(file);
            readDimensions = new Scanner(file);
        }
        catch(FileNotFoundException e) {System.out.println(e.getMessage());}
        dimensions = getDimensions(readDimensions);
        values = new String[dimensions[0]][dimensions[1]];
        
        //reading values from csv file, non-existant values become "".
        readFile.useDelimiter(",");
        int valuesInRow;
        String row;
        for(int i = 0; i < dimensions[0]; i++){
            valuesInRow = 0;
            row = readRows.nextLine();
            for(int j = 0; j < row.length(); j++){
                if(row.charAt(j) == ',') valuesInRow++;
            }
            for(int j = 0; j < dimensions[1]; j++){
                if(j < valuesInRow) values[i][j] = readFile.next();
                else values[i][j] = "";
            }
        }
        readFile.close();
        readRows.close();
    }
    
    //Converts 2D array to 2D list.
    public static List<List<Object>> arrayToList(Object[][] arr){
        List<List<Object>> result = new ArrayList<List<Object>>(arr.length);
        for(int i = 0; i < arr.length; i++){
            result.add(new ArrayList<Object>(arr[0].length));
            for(int j = 0; j < arr[0].length; j++)
                result.get(i).add(arr[i][j]);
        }
        return result;
    }
    
    //Columns = Number of values in row with most values.
    public int[] getDimensions(Scanner scan){
        int cols = 0, rows = 0, colsInRow;
        String line;
        while(scan.hasNextLine()){
            rows++;
            colsInRow = 0;
            line = scan.nextLine();
            for(int i = 0; i < line.length(); i++)
                if(line.charAt(i) == ',') colsInRow++;
            cols = Math.max(cols, colsInRow);
        }
        int[] result = {rows, cols};
        scan.close();
        return result;
    }
    
    //Get values of Csv object.
    public String[][] getValues(){
        return values;
    }
}
