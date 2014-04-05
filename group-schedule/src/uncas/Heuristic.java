package uncas;

import org.junit.Test;

import java.io.*;

/* 
 * This runs on all traces and prints heuristics policy results to files
 */

public class Heuristic
{
    public enum HeuristicType
    {
            heuristic_ALL(0),//this option runs the code with each of the next heuristic types
            heuristic_AGE(1),//oldest instruction first
            heuristic_LATENCY(2),//longest instruction first
            //adding the sequence number to latency
            heuristic_LATENCY_SNR(4),
            //the score functions
            heuristic_BASE_SCORE(5),//score including only insn type
            heuristic_SCORE(6),//score including branch prediction accuracy 
            heuristic_LOADS_FIRST(7),//further prioritize loads
            heuristic_LOAD_MISSED (8),//prioritize loads following a miss
            heuristic_LOADS_FIRST_MISSED(9);//prioritize all loads once and further prioritize loads following a miss
        
        // Add new heuristics here
        
        public final int value;
        
        private HeuristicType(int value) {
            this.value = value;
        }
        static public void printAll() {
            for (HeuristicType type : HeuristicType.values()) {
                System.out.format("Type %d: %s\n", type.value, type.toString());
            }
        }
        static public void printAllToFile(BufferedWriter out){
            for (HeuristicType type : HeuristicType.values()) {
                try{
                    out.write("Type " + type.value +": " + type.toString()+"\n");
                }
                catch (Exception e){//Catch exception if any
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }
    
    public HeuristicType type;
    public Heuristic(HeuristicType hType)
    {
        type = hType;
    }
    public void Print()
    {
        System.out.format("%s", this.type.toString());
    }
}