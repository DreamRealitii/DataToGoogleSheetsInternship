import java.io.*;
import java.util.*;

//SID Stands for "Spreadsheet ID".
//This class writes a single line of text into a text file.
public class SID{
    //Takes a string and writes a text file in the directory you run it in.
    public static void writeTextFile(String id, String fileName){
        try {
            PrintWriter writer = new PrintWriter(fileName, "UTF-8");
            writer.print(id);
            writer.close();
        } catch(Exception e) {System.out.println(e.getMessage());}
    }
    
    //Takes a filepath and returns the first line as a string.
    public static String readTextFile(String filepath){
        String result = "";
        try {
            Scanner reader = new Scanner(new File(filepath));
            result = reader.next();
            reader.close();
        } catch(Exception e) {System.out.println(e.getMessage());}
        return result;
    }
}