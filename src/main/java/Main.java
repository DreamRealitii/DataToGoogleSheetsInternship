import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPin;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinDirection;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.trigger.GpioCallbackTrigger;
import com.pi4j.io.gpio.trigger.GpioPulseStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSetStateTrigger;
import com.pi4j.io.gpio.trigger.GpioSyncStateTrigger;
import com.pi4j.io.gpio.event.GpioPinListener;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.io.gpio.event.PinEventType;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

/**
 * Write a description of class Main here.
 * https://askubuntu.com/questions/101746/how-can-i-execute-a-jar-file-from-the-terminal
 *
 * When new sensors are added edit "//SensorEditRequired" areas to implement them
 *
 * @author Kevin Sikes (Edited by Ethan Carnahan)
 * @version 2/20/2019
 */
public class Main
{
    private static int sleepTime = 10000;
    private static int baseSleepTime = 10000;
    private static Device[] devices = new Device[30];
    private static boolean[] devicesToCheck = new boolean[30];
    private static int numberOfDevices = 0;
    private static Sensor[] sensors = new Sensor[30];
    private static int numberOfSensors = 0;
    private static double systemVoltage;
    //private ArrayList<String> data;
    //private ArrayList<String> previousData;
    private static ArrayList<String> data = new ArrayList<String>();
    private static ArrayList<String> previousData= new ArrayList<String>();
    //private String[] preDefinedSensorNames = new String[1];
    private static String[] preDefinedSensorNames = {"dht11","SoilHumidity"};
    private static boolean firstRun = true;
    private static boolean testedActions = false;
    ///
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    private static Date time;
    private static boolean timeToTurnOnDevice;/////
    private static boolean timeToTurnOffDevice;/////
    private static ArrayList<Device> deviceToTurnOnTime = new ArrayList<Device>(20);/////
    private static ArrayList<Device> deviceToTurnOffTime = new ArrayList<Device>(20);/////

    private static DiscordBot bot;
    private static SMS texter;
    
    static final GpioController gpio = GpioFactory.getInstance();

    /**
     * Constructor for objects of class Main
     * Promts user for variables, writes them to a file, and then runs everything else
     */
    public static void main(String[] args){
        /**
        // setup wiringPi
        if (Gpio.wiringPiSetup() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            return;
        }

        GpioUtil.export(3, GpioUtil.DIRECTION_OUT);*/

        //setting up pre-defined sensor names
        //preDefinedSensorNames[0] = "dht11";
        //initializing variables
        boolean exit = false;
        boolean readConfig = false;
        //data = new ArrayList<String>();
        //previousData = new ArrayList<String>();
        //sleepTime = 10000;
        System.out.println("Staring Setup");
        try{
            File file = new File("/home/pi/Desktop/i-61 config.csv");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while(!reader.readLine().equals("")){;}
            int numSensors=2;
            int numDevices=3;
            String num = reader.readLine();
            numSensors = Integer.parseInt(num);
            num = reader.readLine();
            numDevices = Integer.parseInt(num);

            //setting up sensors
            for(int k = 0; k<numSensors;k++){
                String[] input = reader.readLine().split(",");
                int[] typeAry;
                String[] typeNameAry;
                switch(input[0]) {//SensorEditRequired
                    case "0":
                        typeAry = new int[]{0, 1};
                        typeNameAry = new String[]{"Humidity", "Temperature"};
                        sensors[numberOfSensors] = new TemperatureTest(32.0f, 122.0f, typeAry, typeNameAry, Integer.parseInt(input[1]));
                        numberOfSensors++;
                        break;
                    case "1":
                        typeAry = new int[]{0};
                        typeNameAry = new String[]{"Soil Humidity"};
                        sensors[numberOfSensors] = new SoilHumiditySensor(100.0f, 0.0f, typeAry, typeNameAry, Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                        numberOfSensors++;
                        break;
                    default:
                        break;
                }
            }
            //sleep time
            String in =reader.readLine();
            sleepTime = Integer.parseInt(in)*60*1000;
            baseSleepTime = sleepTime;
            //setting up devices
            //device,sensorControlled,sensor,dataType,criticalPoint,reactAboveValue,aboveValue,reactBelowValue,belowValue,pin,timeControl,times,onFor,notifyMode,intervalCounter,warningCounter
            boolean sensorControlled = false;
            Sensor controller = null;
            int sensorDataType = 0;
            float criticalPoint = 0f;
            boolean takeActionUp =false;
            float upperActionBound = 0f;
            boolean takeActionLow = false;
            float lowerActionBound = 0f;
            int pin;
            boolean timeControl = false;
            String[] times = {};
            String onFor = "";
            String notifyMode = "INTERVAL";
            int intervalCounter = 1;
            int warningCounter = 1;

            float powerUsage;
            for(int x = 0; x<numDevices;x++){
                String[] input = reader.readLine().split(",");
                if(input[0].equals("true")) {
                    sensorControlled = input[1].equals("true");
                    if(sensorControlled) {
                        controller = sensors[Integer.parseInt(input[2])];
                        sensorDataType = Integer.parseInt(input[3]);
                        criticalPoint = Float.parseFloat(input[4]);
                        takeActionUp = input[5].equals("true");
                        if(takeActionUp)
                            upperActionBound = Float.parseFloat(input[6]);
                        takeActionLow = input[7].equals("true");
                        if(takeActionLow)
                            lowerActionBound = Float.parseFloat(input[8]);
                    }
                    pin = Integer.parseInt(input[9]);
                    timeControl = input[10].equals("true");
                    if(timeControl) {
                        times = input[11].split(";");
                        onFor = input[12];
                    }
                    
                    notifyMode = input[13];
                    intervalCounter = Integer.parseInt(input[14]);
                    warningCounter = Integer.parseInt(input[14]);

                    powerUsage = 0.0f;

                    Device d = new Device(sensorControlled, controller, sensorDataType, criticalPoint, takeActionUp, upperActionBound, takeActionLow, lowerActionBound, pin, gpio, timeControl, times, onFor, notifyMode, intervalCounter, warningCounter);
                    for (int z = 0; z < devices.length - 1; z++) {
                        if (devices[z] == null) {
                            devices[z] = d;
                            break;
                        }
                    }
                }
            }
            args = reader.readLine().split(",");
            if(args.length < 5) throw new IllegalArgumentException("Make sure there is at least a space for each value in the bottom row.");
            readConfig = true;
        }
        catch (Exception e){
            System.out.println("Setup from config file failed. Please check file or complete the following prompts");
            System.out.println(e.toString());
            e.printStackTrace();
        }
        if(!readConfig) {
            Scanner in = new Scanner(System.in);
            String statement;
            int sensorPromts;
            time = new Date(System.currentTimeMillis());
            System.out.println("This is the current System Time: " + sdf.format(time) + "\n if this is not your current time correct it in your system settings");
            while (true) {
                System.out.println("How many sensors will you use? Ex: 2");
                statement = in.nextLine();
                try {
                    if (Integer.parseInt(statement) > 0) {
                        sensorPromts = Integer.parseInt(statement);
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Please answer with a valid number > 0");
                }
            }

            //code in prompts and file writer
            for (int x = 0; x < sensorPromts; x++) {
                sensorPrompt(in);
            }

            while (true) {
                System.out.println("How often (in minutes) do you want all of the sensors to take a reading? \n (If there are multiple times that you want, pick the smallest time)\n Ex: 2");
                statement = in.nextLine();
                try {
                    if (Integer.parseInt(statement) > 0) {
                        sleepTime = Integer.parseInt(statement) * 60 * 1000;
                        baseSleepTime = sleepTime;
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Please answer with a valid number > 0");
                }
            }

            while (true) {
                System.out.println("Do you want to add a device to the system? (yes/no)");
                statement = in.nextLine();
                if (statement.toLowerCase().equals("yes")) {
                    devicePrompt(in);
                } else if (statement.toLowerCase().equals("no")) {
                    break;
                } else {
                    System.out.println("Please answer with \'yes\' or \'no\'");
                }
            }
        }
        try{
            if(args[0].length() > 1) bot = new DiscordBot().setupBot(args[0]);
            if(args[1].length() > 1 && args[2].length() > 1 && args[3].length() > 1 && args[4].length() > 1) texter = new SMS(args[1], args[2], args[3], args[4]);
            SheetsQuickstart.setup();
        } catch(Exception e){System.out.println("Something went wrong with Google/Discord:\n" + e.getMessage());}
        System.out.println("Setup complete!");
        notify(args, "Raspberry Pi has started collecting data");

        int timeHold = 0;
        boolean heldTime = false;
        while(!exit){
            try{run(args);}
            catch(Exception e){System.out.println("Something went wrong:\n" + e.getMessage());}
            if(sleepTime>baseSleepTime){
                timeHold = sleepTime - baseSleepTime;
                sleepTime = sleepTime - timeHold;
                heldTime = true;
            }
            if(testedActions){/////
                try{
                    Thread.sleep(sleepTime);
                }
                catch(Exception e){
                    System.out.println("Sleep Time Failure");
                    System.out.println(e.getStackTrace());
                }

                for(int i = 0;i<devices.length;i++){
                    if(devicesToCheck[i]){
                        float lastDataPoint = devices[i].lastDataPoint;
                        float currentDataPoint = devices[i].getNewCurrentDataPoint();
                        float criticalPoint = devices[i].criticalPoint;
                        if(Math.abs(criticalPoint-lastDataPoint) > Math.abs(criticalPoint-currentDataPoint)){
                            System.out.println("The value that device " + i + "on pin " + devices[i].pin + " depends on is farther away from the critical point than the last value: \n last value: " + lastDataPoint + " Current value: " + currentDataPoint + " Critical Point: " + criticalPoint);
                        }
                    }
                }
            }
            else{
                try{
                    Thread.sleep(sleepTime);
                }
                catch(Exception e){
                    System.out.println("Sleep Time Failure");
                    System.out.println(e.getStackTrace());
                }
            }

            if(!heldTime) {
                if (timeToTurnOnDevice) {
                    for (Device d : deviceToTurnOnTime) {
                        d.turnOn();
                    }
                }
                if (timeToTurnOffDevice) {
                    for (Device d : deviceToTurnOffTime) {
                        d.turnOff();
                    }
                }

                getNextSleepTime();
            }
            else {
                sleepTime = timeHold;
                heldTime = false;
            }


            /**if(in.hasNext()){
                if(in.nextLine().toLowerCase() == "exit"){
                    exit=true;
                }
            }*/
          }
    }

    private static void getNextSleepTime(){
        time = new Date(System.currentTimeMillis());
        String currentTime = sdf.format(time);
        String[] currentHourAndMinutes = currentTime.split(":");
        int currentHour = Integer.parseInt(currentHourAndMinutes[0]) *60*60*1000;
        int currentMinute = Integer.parseInt(currentHourAndMinutes[1]) *60*1000;
        int theTime = currentHour + currentMinute;
        int closestTime = -1;
        for(Device d:devices) {
            if (d != null) {
                if (d.timeControl) {
                    for (String s : d.times) {
                        String[] hoursAndMinutes = s.split(":");
                        int timeAway = (Integer.parseInt(hoursAndMinutes[0]) * 60 * 60 * 1000) + (Integer.parseInt(hoursAndMinutes[1]) * 60 * 1000) - theTime;
                        if (timeAway < baseSleepTime && timeAway >= 0) {
                            if (closestTime == -1) {
                                closestTime = timeAway;
                                deviceToTurnOnTime.add(d);
                                timeToTurnOnDevice = true;
                            } else if (timeAway < closestTime) {
                                closestTime = timeAway;
                                deviceToTurnOnTime.add(d);
                                timeToTurnOnDevice = true;
                                deviceToTurnOffTime.clear();
                                timeToTurnOffDevice = false;
                            } else if (timeAway == closestTime) {
                                closestTime = timeAway;
                                deviceToTurnOnTime.add(d);
                                timeToTurnOnDevice = true;
                            }
                        }
                    }
                    if (d.deviceState()) {
                        /////
                        String[] dOnFor = d.onFor.split(":");
                        int dTimeAway = (Integer.parseInt(dOnFor[0]) * 60 * 60 * 1000) + (Integer.parseInt(dOnFor[1]) * 60 * 1000);
                        if (closestTime == -1) {
                            closestTime = dTimeAway;
                            deviceToTurnOffTime.add(d);
                            timeToTurnOffDevice = true;
                        } else if (closestTime > dTimeAway /**&& baseSleepTime>dTimeAway need to store how much time has passed*/) {
                            closestTime = dTimeAway;
                            deviceToTurnOffTime.add(d);
                            timeToTurnOffDevice = true;
                            deviceToTurnOnTime.clear();
                            timeToTurnOnDevice = false;
                        } else if (closestTime == dTimeAway) {
                            closestTime = dTimeAway;
                            deviceToTurnOffTime.add(d);
                            timeToTurnOffDevice = true;
                        }
                    }
                }
                if (closestTime == -1) {
                    sleepTime = baseSleepTime;
                    timeToTurnOnDevice = false;
                    deviceToTurnOnTime.clear();
                } else {
                    sleepTime = closestTime;
                }
            }
        }
    }

    private static void sensorPrompt(Scanner in){
        String statement;
        while(true){
            System.out.println("Which sensor will you be using? (type the number by the sensor type)");
            int x = 0;
            for(String s:preDefinedSensorNames){
                if(s != null){
                    System.out.println(x + ": " + s);
                }
                x++;
            }
            statement = in.nextLine();
            try{//SensorEditRequired
                boolean notASensor = false;
                int pin;
                int[] typeAry;
                String[] typeNameAry;
                int channel;
                switch(Integer.parseInt(statement)){
                    case 0:
                        typeAry = new int[]{0,1};
                        typeNameAry = new String[]{"Humidity","Temperature"};
                        while(true){
                            System.out.println("What GPIO pin will the sensor use? (EX: 21)");
                            statement = in.nextLine();
                            try{
                                pin = Integer.parseInt(statement);
                                break;
                            }
                            catch(Exception e){
                                System.out.println("Please answer with a valid number");
                            }
                        }
                        sensors[numberOfSensors] = new TemperatureTest(32.0f,122.0f,typeAry ,typeNameAry,pin);
                        numberOfSensors++;
                        break;
                    case 1:
                        typeAry = new int[]{0};
                        typeNameAry = new String[]{"Soil Humidity"};
                        while(true){
                            System.out.println("What GPIO pin will the sensor use? (EX: 21)");
                            statement = in.nextLine();
                            try{
                                pin = Integer.parseInt(statement);
                                break;
                            }
                            catch(Exception e){
                                System.out.println("Please answer with a valid number");
                            }
                        }
                        while(true){
                            System.out.println("What channel will the Soil Humidity Sensor use? (EX: 0)");
                            statement = in.nextLine();
                            try{
                                channel = Integer.parseInt(statement);
                                break;
                            }
                            catch(Exception e){
                                System.out.println("Please answer with a valid number");
                            }
                        }
                        sensors[numberOfSensors] = new SoilHumiditySensor(100.0f,0.0f,typeAry ,typeNameAry,pin,channel);
                        numberOfSensors++;
                        break;
                    default: System.out.println("Please answer with a valid number");
                        notASensor=true;
                        break;
                }
                if(notASensor){
                    System.out.println("Please answer with a valid integer");
                }
                else{
                    break;
                }
            }
            catch(Exception e){
                System.out.println("Please answer with a valid number");
            }
        }
    }
    
    /**
     * prompt devices only after ALL sensors have been declared
     */
    private static void devicePrompt(Scanner in){
        String statement;
        Device d;
        Sensor controller = null;
        int sensorDataType = 0;
        boolean sensorControlled;
        boolean takeActionUp = false;
        boolean takeActionLow = false;
        boolean timeControl = false; ///
        String[] times = new String[0];///
        String onFor = "";///
        float upperActionBound = 0.0f;
        float lowerActionBound = 0.0f;
        float criticalPoint = 0.0f;
        float powerUsage = 0.0f;
        int pin = 0;
        String notificationMode = "INTERVAL";
        int intervalCounter = 1;
        int warningCounter = 1;
        ///System.out.println(sdf.format(new Date(System.currentTimeMillis())));
        while(true){
            System.out.println("Will this device be controlled by a sensor?(Yes/No)");
            statement = in.nextLine().toLowerCase();
            if(statement.equals("yes")){
                sensorControlled = true;
                break;
            }
            else if(statement.equals("no")){
                sensorControlled = false;
                break;
            }
            else{
                System.out.println("Please answer with \'Yes\' or \'No\'");
            }
        }
        ///vvv
        while(!sensorControlled){
            ///sdf.format(new Date(System.currentTimeMillis()));
            System.out.println("Will the device be time controlled? (Yes/No)");
            statement = in.nextLine().toLowerCase();
            if(statement.equals("yes")){
                timeControl = true;
                while(true){
                    int promptNum;
                    System.out.println("How many different times will it turn on a day (24 hour period)? (#) ex: 2");
                    statement = in.nextLine().toLowerCase();
                    try{
                        promptNum = Integer.parseInt(statement);
                        times = new String[promptNum];
                        for(int x = 0;x<promptNum;x++){
                            while(true){
                                System.out.println("At what times does  the device turn on? (HH:MM) Prompt: " + (x+1) + "\n Ex: 15:30 will turn the device on at 3:30");
                                statement = in.nextLine().toLowerCase();
                                if(statement.matches("\\d{2}:\\d{2}")){
                                    times[x] = statement;
                                    break;
                                }
                                else{
                                    System.out.println("Please enter a valid time");
                                }
                            }
                        }
                        while(true){
                            System.out.println("How long should the device stay on for? (HH:MM) \n example: 01:15 will be on for 1 hour and 15 minutes after starting");
                            statement = in.nextLine().toLowerCase();
                            if(statement.matches("\\d{2}:\\d{2}")){
                                onFor = statement;
                                break;
                            }
                            else{
                                System.out.println("please answer with a valid time");
                            }
                        }
                        break;
                    }
                    catch (Exception e){
                        System.out.println(e.getStackTrace().toString());
                        System.out.println("please enter a valid integer");////
                    }
                }
                break;
            }
            else if(statement.equals("no")){
                break;
            }
            else{
                System.out.println("please answer with \'yes\' or \'no\'");
            }
        }
        ///^^^
        while(sensorControlled){
            System.out.println("Which sensor will control the device?(type the number of the sensor from the given list)");
            for(int x = 0;x<sensors.length-1;x++){
                if(sensors[x] != null){
                    System.out.println(x + ":" + sensors[x].getName());
                }
            }
            statement = in.nextLine();
            try{
                if(sensors[Integer.parseInt(statement)] !=null){
                    controller = sensors[Integer.parseInt(statement)];
                    while(controller.types.length>1){
                        System.out.println("What data type will the device use?");
                        /**for(int x = 0;x<controller.types.length;x++){
                            if(sensors[x] != null){
                                System.out.println(x + ":" + controller.typesName[x]);
                            }
                        }*/
                        int x = 0;
                        for(String s:controller.typesName){
                            System.out.println(x + ":" + s);
                            x++;
                        }
                        statement = in.nextLine();
                        try{
                            if(Integer.parseInt(statement)<controller.types.length&&Integer.parseInt(statement)>=0){
                                sensorDataType = controller.types[Integer.parseInt(statement)];
                                break;
                            }
                            else{
                                System.out.println("Please pick one of the data types shown");
                            }
                        }
                        catch(Exception e){
                            System.out.println("Please answer with a valid number");
                        }
                    }
                    while(true){
                        System.out.println("Does the device need to react if the data from the sensor is above a certain value? (yes/no)");
                        statement = in.nextLine();
                        if(statement.toLowerCase().equals("yes")){
                            takeActionUp=true;
                            while(true){
                                System.out.println("What is the value? EX: 74.3");
                                statement = in.nextLine();
                                try{
                                    upperActionBound = Float.parseFloat(statement);
                                    break;
                                }
                                catch(Exception e){
                                    System.out.println("Please answer with a valid number containing a decimal");
                                }
                            }
                            break;
                        }
                        else if(statement.toLowerCase().equals("no")){
                            takeActionUp=false;
                            break;
                        }
                        else{
                            System.out.println("Please answer with \'yes\' or \'no\'");
                        }
                    }
                    while(true){
                        System.out.println("Does the device need to react if the data from the sensor is below a certain value? (yes/no)");
                        statement = in.nextLine();
                        if(statement.toLowerCase().equals("yes")){
                            takeActionLow=true;
                            while(true){
                                System.out.println("What is the value? EX: 73.2");
                                statement = in.nextLine();
                                try{
                                    lowerActionBound = Float.parseFloat(statement);/**could search to see if there iss a decimal and if not add it myself*/
                                    break;
                                }
                                catch(Exception e){
                                    System.out.println("Please answer with a valid number containing a decimal");
                                }
                            }
                            break;
                        }
                        else if(statement.toLowerCase().equals("no")){
                            takeActionLow=false;
                            break;
                        }
                        else{
                            System.out.println("Please answer with \'yes\' or \'no\'");
                        }
                    }
                    while(true){
                        System.out.println("What sensor value is best for the device to turn off at? \n EX: An air conditioner set to turn off when a room cools to 75.0 degrees farenheight");
                        statement = in.nextLine();
                        try{
                            criticalPoint = Float.parseFloat(statement);
                            break;
                        }
                        catch(Exception e){
                            System.out.println("Please answer with a valid number containing a decimal");
                        }
                    }
                    break;
                }
                else{
                    System.out.println("Please pick one of the sensors shown");
                }
            }
            catch(Exception e){
                System.out.println("Please answer with a valid number");
            }
        }
        while(true){
            System.out.println("How much power does the device use in Amps/Hour? EX: 3.1");
            statement = in.nextLine();
            try{
                powerUsage = Float.parseFloat(statement);
                break;
            }
            catch(Exception e){
                System.out.println("Please answer with a valid number containing a decimal");
            }
        }
        while(true){
            System.out.println("What GPIO pin will the sensor use? (EX: 21)");
            statement = in.nextLine();
            try{
                pin = Integer.parseInt(statement);
                break;
            }
            catch(Exception e){
                System.out.println("Please answer with a valid number");
            }
        }
        while(true){
            System.out.println("What notification mode will the device use? Use \"interval\", \"warning\", or \"crossing\"");
            statement = in.nextLine().toUpperCase();
            try{
                if(statement.equals("INTERVAL") || statement.equals("WARNING") || statement.equals("CROSSING")){
                    notificationMode = statement;
                    break;
                }
            }
            catch(Exception e){
                System.out.println("Please answer with a valid word");
            }
        }
        while(true){
            System.out.println("At what interval will warnings trigger notifications (for interval mode)?");
            statement = in.nextLine();
            try{
                intervalCounter = Integer.parseInt(statement);
                break;
            }
            catch(Exception e){
                System.out.println("Please answer with a valid number");
            }
        }
        while(true){
            System.out.println("After how many warnings will notifications begin being sent (For warning mode)?");
            statement = in.nextLine();
            try{
                warningCounter = Integer.parseInt(statement);
                break;
            }
            catch(Exception e){
                System.out.println("Please answer with a valid number");
            }
        }
        //**g
        d = new Device(sensorControlled,controller,sensorDataType,criticalPoint,takeActionUp,upperActionBound,takeActionLow,lowerActionBound,pin,gpio, timeControl, times, onFor, notificationMode, intervalCounter, warningCounter);
        for(int x = 0;x<devices.length-1;x++){
            if(devices[x] == null){
                devices[x] = d;
                break;
            }
        }
        //*/
    }

    //This method runs on an infinite loop after the setup.
    private static void run(String[] args) throws Exception{
        getSensorData();
        controlLogic(args);
        testActions();
        String id = SID.readTextFile("spreadsheetID");
        if(id.equals("")) {
            System.out.println("Spreadsheet was not found or another error has occured:\n");
            id = SheetsQuickstart.createSpreadsheet("/home/pi/Desktop/SensorData.csv");
        }
        SheetsQuickstart.updateSpreadsheet("/home/pi/Desktop/SensorData.csv", id);
    }

    //This method always writes into the log file, may send notifications depending on what's in the config file.
    public static void notify(String[] args, String message){
        try {
            LogFile.writeLog(message);
            if(args[0].length() > 1) bot.sendMessage(message);
            if(args[1].length() > 1 && args[2].length() > 1 && args[3].length() > 1 && args[4].length() > 1) texter.sendText(message);
        } catch(Exception e) {System.out.println("Error occurred sending notifications:\n" + e.getMessage());}
    }
    
    public static void notify(String[] args, int action, Device device){
        String message = "";
        switch(action){
            case 1: message = "Device " + device.controller.name + " is turning on due to high data values.";
            break;
            case 2: message = "Device " + device.controller.name + " is turning on due to low data values.";
            break;
            case 3: message = "Device " + device.controller.name + " needs to turn off.";
        }
        if(action > 0 && device.count()){    
            try {
                LogFile.writeLog(message);
                if(args[0].length() > 1) bot.sendMessage(message);
                if(args[1].length() > 1 && args[2].length() > 1 && args[3].length() > 1 && args[4].length() > 1) texter.sendText(message);
            } catch(Exception e) {System.out.println("Error occurred sending notifications:\n" + e.getMessage());}
        } else if(device.notifyMode.equals("CROSSING") && !device.crossed && action > 0){
            device.crossed = true;
            try {
                LogFile.writeLog(message);
                if(args[0].length() > 1) bot.sendMessage(message);
                if(args[1].length() > 1 && args[2].length() > 1 && args[3].length() > 1 && args[4].length() > 1) texter.sendText(message);
            } catch(Exception e) {System.out.println("Error occurred sending notifications:\n" + e.getMessage());}
        } else if(device.notifyMode.equals("CROSSING") && device.crossed && action == 0){
            device.crossed = false;
            message = "Device " + device.controller.name + " has returned to a safe range.";
            try {
                LogFile.writeLog(message);
                if(args[0].length() > 1) bot.sendMessage(message);
                if(args[1].length() > 1 && args[2].length() > 1 && args[3].length() > 1 && args[4].length() > 1) texter.sendText(message);
            } catch(Exception e) {System.out.println("Error occurred sending notifications:\n" + e.getMessage());}
        }
    }
    
    private static void getSensorData(){
        if(!firstRun){
            previousData = data;
            data = new ArrayList<String>();
        }
        else{
            firstRun = false;
        }
        for(Sensor s:sensors){//SensorEditRequired
            if(s != null){
                switch(s.getName()){
                    case "dht11":
                        data.add(/**"Humidity:" +*/ s.readData(0) +"");//Humidity
                        data.add(/**"Temperature:" + */s.readData(1) +"");//Temperature
                        break;
                    case "SoilHumidity":
                        data.add(/**"SoilHumidity:" +*/ s.readData(0) +"");
                        break;
                    }
                }
            else{
                break;
            }
        }
        try{
            FileWriter writer;
            File file = new File("/home/pi/Desktop/SensorData.csv");
            writer = new FileWriter(file,true);
            Date dateAndTime = new Date(System.currentTimeMillis());
            writer.append(dateAndTime.toString() + ",");
            System.out.println(dateAndTime.toString());
            for(String s:data){
                if(!s.equals("")){
                    writer.append(s + ",");
                    System.out.println(s + "\n");
                }
            }
            writer.append("\n");
            writer.flush();
            writer.close();
        }
        catch(IOException e){
            System.out.println("Data write error");
            e.printStackTrace();
        }
    }
    
    private static void controlLogic(String[] args){
        int action;
        int powerAction = predictEnergyConsumptionAndAction();
        int deviceNum = 0;
        String deviceName;
        for(Device d:devices){
            if(d!=null){
                deviceName = d.controller.name;
                action = d.needsAction();
                if(action == 1){//upper action
                    d.turnOn();
                    devicesToCheck[deviceNum] = true;
                }
                else if (action == 2){//lower action
                    d.turnOn();
                    devicesToCheck[deviceNum] = true;
                }
                else if (action == -1){//action determined by power
                    if(powerAction == 1){
                        d.turnOff();
                        devicesToCheck[deviceNum] = true;
                    }
                    else{
                        d.turnOn();
                        devicesToCheck[deviceNum] = false;
                    }
                }
                else if (action == 3){
                    d.turnOff();
                    devicesToCheck[deviceNum] = false;
                }
                notify(args, action, d);
                deviceNum++;
            }
            else{
                break;
            }
        }
    }
    
    /**
     * return 0 if ok on power
     * return 1 if power consumption is too high
     */
    private static int predictEnergyConsumptionAndAction(){
        return 0;
    }
    
    /**
     * compares the last data point with a more current one to the critical point
     * This will warn the user if the current data point is farther than the last data point from the critical point
     */
    private static void testActions(){/////
        boolean needToTestActions = false;
        testedActions = false;
        for(boolean b:devicesToCheck){
            if(b){
                needToTestActions = true;
            }
        }
        if(needToTestActions){
            testedActions = true;
        }
    }
}
