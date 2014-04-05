package uncas;

import uncas.FrequencyHistogram;
import uncas.Uop.UopType;

import java.lang.String;
import java.util.zip.GZIPInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/*
 * Use this to find the instruction mix
 */

public class InsnMix
{    
    
    public BufferedReader traceReader = null;
    
    // Program Stats
    public long totalUops = 0;
    public long totalMops = 0;
    
    // Question 4A
    public FrequencyHistogram InsnTypeFrequencyHistogram = new FrequencyHistogram("Frequency of Each Uop Type", "uops of type", ""); 
    
    /*
     * Constructor for the simulator.
     * @param traceFile the name of the trace file you wish to simulate. in .gz format
     */
    public InsnMix(String traceFile) throws IOException {
        // ugly, but JAVA IO is not the point
        traceReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(traceFile))));
    }
    
    /*
     * Goes through the trace line by line (i.e., instruction by instruction), examining
     * each instruction in the trace. 1 line = 1 micro-op (micro-op = uop)
     */
    public void simulate() throws IOException  {
        String line;
        long prevInst = 0;
        long insnBytes = 0;
        long numBits = 0;
        boolean isPrevWrite = false;
        System.out.format("\n\n");
        
        while (true) {
            line = traceReader.readLine();
            if (line == null) {
                break;
            }
            
            // highly recommend you look in the construction in HW1Uop.java to see what it does!
            Uop currUop = new Uop(line,1);
            
            // each line of the trace is a micro-op, so increment each time
            totalUops++;
            
            // macro-ops can be counted by counting the first micro-op in the macro-op
            // this is identified by microOpCount == 1
            if (currUop.microOpCount == 1) {
                totalMops++;    
            }
            
            InsnTypeFrequencyHistogram.increment(currUop.type.value);
            /*if(currUop.type.value == 6)
                System.out.format("%s\n", line);*/
        }
        
    }
    
    
}
