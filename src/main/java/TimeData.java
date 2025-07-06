import java.util.*;

//Separates out the day, month, hour, etc. into their own value columns.
public class TimeData
{
    private List<List<String>> timeData;
    private List<List<Object>> timeObjectData;
    private static Scanner read;

    //Takes the raw data to create the TimeData.
    public TimeData(String[][] rawData)
    {
        timeData = new ArrayList<>();
        
        //Creates label row.
        List<String> labelRow = new ArrayList<>();
        String[] labels = {"Time", "Second", "Minute", "Hour", "Day of Week", "Time Zone", "Day of Month", "Month", "Year", "Temperature", "Humidity", "Soil Moisture"};
        for(String label : labels)
            labelRow.add(label);
        for(int i = 4; i < rawData[0].length; i++)
            labelRow.add("Other Data " + String.valueOf(i));
        timeData.add(labelRow);
            
        //Adds data.
        for(String[] row : rawData)
            timeData.add(row(row));
        timeObjectData = getObjectList(timeData);
    }

    //Takes in the raw data and creates a row.
    private static List<String> row(String[] data){
        List<String> result = new ArrayList<>();
        read = new Scanner(data[0]);
        List<String> timeParts = new ArrayList<>();
        while(read.hasNext())
            timeParts.add(read.next());
        result.add(formattedTime(timeParts));
        result.add(timeParts.get(3).substring(6));
        result.add(timeParts.get(3).substring(3, 5));
        result.add(timeParts.get(3).substring(0, 2));
        result.add(day(timeParts.get(0)) + "/" + timeParts.get(0));
        result.add(timeParts.get(4));
        result.add(remove0(timeParts.get(2)));
        result.add(month(timeParts.get(1)));
        result.add(timeParts.get(5));
        for(int i = 1; i < data.length; i++)
            result.add(data[i]);
        return result;
    }
    
    //First column of TimeDate is raw data converted to Google Sheet's time format.
    private static String formattedTime(List<String> parts){
        return month(parts.get(1)) + "/" + remove0(parts.get(2)) + "/" + parts.get(5) + " " + parts.get(3);
    }
    
    //Removes "0" from beginning of day number ("06" becomes "6").
    private static String remove0(String day){
        if(day.charAt(0) == '0') return day.substring(1);
        return day;
    }
    
    //Converts month name to number.
    private static String month(String month){
        switch(month){
            case "Jan": return "1";
            case "Feb": return "2";
            case "Mar": return "3";
            case "Apr": return "4";
            case "May": return "5";
            case "Jun": return "6";
            case "Jul": return "7";
            case "Aug": return "8";
            case "Sep": return "9";
            case "Oct": return "10";
            case "Nov": return "11";
            case "Dec": return "12";
            default: return "Invalid Month";
        }
    }
    
    //Converts day-of-week name to number.
    private static String day(String day){
        switch(day){
            case "Sun": return "1";
            case "Mon": return "2";
            case "Tue": return "3";
            case "Wed": return "4";
            case "Thu": return "5";
            case "Fri": return "6";
            case "Sat": return "7";
            default: return "Invalid Day";
        }
    }
    
    //Used in constructor to create "TimeObjectData".
    public List<List<Object>> getObjectList(List<List<String>> timeData){
        List<List<Object>> result = new ArrayList<List<Object>>(timeData.size());
        for(int i = 0; i < timeData.size(); i++){
            result.add(new ArrayList<Object>(timeData.get(i).size()));
            for(int j = 0; j < timeData.get(i).size(); j++)
                result.get(i).add(timeData.get(i).get(j));
        }
        return result;
    }
    
    public List<List<Object>> getData(){
        return timeObjectData;
    }
}
