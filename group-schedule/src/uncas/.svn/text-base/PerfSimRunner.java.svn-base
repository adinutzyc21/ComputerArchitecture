package uncas;

import uncas.Heuristic;
import uncas.Heuristic.HeuristicType;
import org.junit.Test;

/* 
 * Run this JUnit test, print out the result, and turn it in with the
 * paper portion of this assignment.
 * Here you can see the variables we expect to be meaningfully set.
 * These are the values we will check when we grade your homework.
 */

public class PerfSimRunner {
    static public String testTrace = "sjeng-10M.trace.gz";
    static public int ROBsize = 256;//ROB size
    static public int machWidth = 4;//machine width
    static public int nPregs = 2048;
    static public boolean runningInEclipse = false;
    Heuristic heuristic = new Heuristic(HeuristicType.heuristic_SCORE);// change to one of the heuristics from HeuristicTypeEnum.java file or to heuristic_ALL
    
    // Will run your simulator and then print out all the results.
    @Test
    public void RunPerformanceSimulator() throws Exception {
        //String tracePath = "/project/cec/class/cse560/traces/" + testTrace;
        String tracePath = "traces/" + testTrace;
        
        if (heuristic.type == HeuristicType.heuristic_ALL){
            for (HeuristicType Looptype : HeuristicType.values()){
                if (Looptype == HeuristicType.heuristic_ALL)
                    continue;
                else {
                    PerfSim sim = new PerfSim(tracePath, nPregs);
                    sim.uopLimit = 1000000000;
                    sim.machineWidth = machWidth;
                    sim.perfect_bpred_f = false; // turns B-pred on or off - true turns it off
                    sim.perfectCache_f = false;
                    sim.ROB.setMaxEntries(ROBsize);
                    heuristic.type = Looptype;
                    sim.heuristic = heuristic;
                    sim.simulate();
                }
            }
        }
        else {
            PerfSim sim = new PerfSim(tracePath, nPregs);
            sim.uopLimit = 1000000000;
            sim.machineWidth = machWidth;
            sim.perfect_bpred_f = false; // turns B-pred on or off - true turns it off
            sim.perfectCache_f = false;
            sim.ROB.setMaxEntries(ROBsize);
            sim.heuristic = heuristic;
            sim.simulate();
        }
        
    }
}  