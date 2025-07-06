import java.io.*;
import java.util.Date;

//This class writes messages (with a date/time) into a Log File.
public class LogFile
{
    public static void writeLog(String logText){
        File file = new File("/home/pi/Desktop/LogFile.csv");
        Date dateAndTime = new Date(System.currentTimeMillis());
        System.out.println(dateAndTime.toString() + " : " + logText + "\n");
        try{
            FileWriter writer = new FileWriter(file,true);
            writer.append(dateAndTime.toString() + " : " + logText + "\n");
            writer.flush();
            writer.close();
        } catch(IOException e) {System.out.println("Error writing log file:\n" + e.getMessage());}
    }
}
