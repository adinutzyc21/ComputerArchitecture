package uncas;

import uncas.InsnMix;
import org.junit.Test;
import java.io.*;

/* 
 * This runs on all traces and prints insn mix results to files
 */

public class InsnMixPrint {
    
    static public String[] traceNames = new String[10];
    static public String traceName;
    static public String testTrace;
    
    static public void populateStringArray(){//ordered by size on disk
        traceNames[0]="sjeng-1K";
        traceNames[1]="sjeng-10M";
        traceNames[2]="libquantum-100M";
        traceNames[3]="art-100M";
        traceNames[4]="hmmer-100M";
        traceNames[5]="mcf-100M";
        traceNames[6]="gcc-50M";
        traceNames[7]="sjeng-100M";
        traceNames[8]="go-100M";
        traceNames[9]="sphinx3-100M";//this gives an error
    }
    
    static public void print4A(InsnMix sim) {
        System.out.format("The distribution of instruction types:\n");
        Uop.UopType.printAll();
        sim.InsnTypeFrequencyHistogram.print();
    } 
    
    static public void printToFile(InsnMix sim, String traceName, int i){
        try{
            //create file
            String ii="";
            if(i < 10)
                ii="0"+i+"_";
            else
                ii=i+"_";
            FileWriter fstream = new FileWriter("results/"+ii+traceName+".txt");
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("The distribution of instruction types:\n");
            Uop.UopType.printAllToFile(out);
            sim.InsnTypeFrequencyHistogram.printToFile(out);
            //close the output stream
            out.close();
        }
        catch (Exception e){//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
    }
    
    // Will run your simulator and then print out all the results.
    @Test
    public void printInsnMix() throws Exception {
        populateStringArray();
        for(int i=9; i<10; i++){
            traceName = traceNames[i];
            testTrace = traceName + ".trace.gz";
            String tracePath = "/project/cec/class/cse560/traces/" + testTrace;
            //String tracePath = "traces/" + testTrace;
            
            System.out.format("\nStarting on trace %s.\n",traceName);
            
            InsnMix sim = new InsnMix(tracePath);
            sim.simulate();
            
            //print4A(sim); 
            printToFile(sim, traceName, i+1);
            System.out.format("Done with trace %s.\n",traceName);
        }
    }
}  