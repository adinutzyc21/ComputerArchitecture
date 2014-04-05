package uncas;

import java.util.HashMap;
import java.lang.Long;
import java.io.*;


public class FrequencyHistogram extends HashMap<Long, Long>{
    
    // this makes Eclipse happy. don't ask why
    private static final long serialVersionUID = 1L;
    
    double totalCounts;
    double totalSize;
    String title = "";
    String sizeTitle = "";
    String countUnit = "";
    
    public FrequencyHistogram(String title, String sizeTitle, String countUnit) {
        super();
        totalCounts = 0.0;
        totalSize = 0.0;
        this.title = title;
        this.sizeTitle = sizeTitle;
        this.countUnit = countUnit;
    }
    
    public void increment(int size) {
        increment((long)size);
    } 
    
    public void increment(Long size) {
        totalCounts++;
        totalSize += size;
        Long count = get(size);
        if (count == null) {
            // first time, counter = 1
            put(size, (long)1);
        } else {
            // increment counter
            put(size, count+1);
        }
    }
    
    public double getOnePercent(Long key) {
        return 100*get(key)/totalCounts;
    }    
    
    public void printOnePercent(Long key) {
        double percent = getOnePercent(key);
        System.out.format("  Percent of %s %2d %s: %4.1f%%\n", sizeTitle, key, countUnit, percent);
    }     
    
    public void printOnePercentToFile(Long key,BufferedWriter out) {
        double percent = getOnePercent(key);
        try{
            out.write("  Percent of "+ sizeTitle +" "+key +": " + countUnit + percent+"%\n");
        }
        catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }     
    
    public void print() {
        System.out.format("Distribution of %s in Trace:\n", title);
        for (Long key: keySet()) {
            printOnePercent(key);
        }
    }
    
    public void printToFile(BufferedWriter out) {
        try{
            out.write("Distribution of "+ title+" in Trace:\n");
        }
        catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        for (Long key: keySet()) {
            printOnePercentToFile(key,out);
        }
    }
    
    public double getAverageSize() {
        
        double average = 0.0;
        // go through each key in the set
        for (Long key: keySet()) {
            double percent = get(key)/totalCounts;
            average += key * percent;                
        }
        return average;
    }
    
    
}