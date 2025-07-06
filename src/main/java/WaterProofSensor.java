 

/**
 * https://github.com/countryBumpkin/Projects/blob/master/I61_Internship/DS18B20_TemperatureSensor/DS18B20.cpp
 */
public class WaterProofSensor extends Sensor{
    public String name;
    public float upperBound;//Sensor's physical capibilities
    public float lowerBound;
    public int[] types;
    public String[] typesName;
    public int pin;

    /**
     * Constructor for objects of class Sensor
     */
    public WaterProofSensor(float upperBound, float lowerBound, int[] types, String[]typesName, int pin)
    {
        super(upperBound,lowerBound,types,typesName, pin);
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
        this.types = types;
        this.typesName = typesName;
        this.pin = pin;
    }

    public float readData(int type){
        return 0;
    }

    public String getName(){
        return "";
    }
}
