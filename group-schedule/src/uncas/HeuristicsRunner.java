package uncas;

import org.junit.Test;
import uncas.Heuristic;
import uncas.Heuristic.HeuristicType;
import java.io.*;

/* 
 * This runs on all traces and prints heuristics policy results to files
 */

public class HeuristicsRunner {
    Heuristic heuristic = new Heuristic(HeuristicType.heuristic_ALL);// change to one of the heuristics from HeuristicTypeEnum.java file or to heuristic_ALL 
    public int latencyMultiplier = 3;//also tested 1 and 2
    public int heurType = 1;//1 is Basic Score, 2 is Basic Sore + Sequence Number, 3 is Basic Score = Latency
    
    static public boolean perfectCache = false; //true means we're not using the cache
    static public boolean perfect_bpred = false; //true means we're not using the predictor
    
    static public int ROBsize = 256;//ROB size
    static public int machWidth = 4;//machine width
    static public int cacheSz = 4096;//cache miss penalty
    static public int branchPen = 5;//cache miss penalty
    static public int nPregs = 2048;//number of physical registers
    
    static public String[] traceNames = new String[10];
    static public String traceName;
    static public String testTrace;
    
    static public void populateArray(){//ordered by size on disk
        traceNames[0]="sjeng-1K";
        traceNames[1]="sjeng-10M";
        traceNames[2]="libquantum-100M";//start here
        traceNames[3]="art-100M";
        traceNames[4]="hmmer-100M";//start here
        traceNames[5]="mcf-100M";//stop here
        traceNames[6]="gcc-50M";
        traceNames[7]="sjeng-100M";
        traceNames[8]="go-100M";
        traceNames[9]="sphinx3-100M";
    }
    
    // Will run your simulator and then print out all the results.
    @Test
    public void RunHeursticsSimulator() throws Exception { 
        populateArray();
        
        System.out.format("\nPerfect Cache is %b, Perfect branch predictor is %b, width is %d\n\n",perfectCache,perfect_bpred,machWidth);
        
        for(int i=2; i<10; i++){
            traceName = traceNames[i];
            testTrace = traceName + ".trace.gz";
            
            String tracePath = "/project/cec/class/cse560/traces/" + testTrace;
            //String tracePath = "traces/" + testTrace;
            
            System.out.format("\n************Starting on trace %s.************\n",traceName);
            
            if (heuristic.type == HeuristicType.heuristic_ALL) {
                for (HeuristicType Looptype : HeuristicType.values())
                {
                    if(perfectCache){//if we have a perfect cache it makes no sense to run these heuristics
                        if(Looptype == HeuristicType.heuristic_LOAD_MISSED || Looptype == HeuristicType.heuristic_LOADS_FIRST_MISSED)
                            continue;
                    }
                    if (Looptype == HeuristicType.heuristic_ALL)
                        continue;
                    else
                    {
                        PerfSim sim = new PerfSim(tracePath, nPregs);
                        sim.uopLimit = 1000000000;
                        sim.machineWidth = machWidth;
                        sim.bpredPenalty = branchPen;
                        sim.latencyMultiplier = latencyMultiplier;
                        sim.heurType = heurType;
                        sim.perfect_bpred_f = perfect_bpred; 
                        sim.perfectCache_f = perfectCache;
                        sim.ROB.setMaxEntries(ROBsize);
                        heuristic.type = Looptype;
                        sim.heuristic = heuristic;
                        sim.simulate();
                    }
                    // setting back to all for next loop
                    heuristic.type = HeuristicType.heuristic_ALL;
                }
            }
            else
            {
                PerfSim sim = new PerfSim(tracePath, nPregs);
                sim.uopLimit = 1000000000;
                sim.machineWidth = machWidth;
                sim.cacheSz = cacheSz;
                sim.bpredPenalty = branchPen;
                sim.latencyMultiplier = latencyMultiplier;
                sim.heurType = heurType;
                sim.perfect_bpred_f = perfect_bpred; 
                sim.perfectCache_f = perfectCache;
                sim.ROB.setMaxEntries(ROBsize);
                sim.heuristic = heuristic;
                sim.simulate();
            }
            System.out.format("Done with trace %s.\n",traceName);
        }
    }
}  
