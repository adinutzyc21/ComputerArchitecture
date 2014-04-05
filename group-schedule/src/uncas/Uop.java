package uncas;

import java.io.*;

public class Uop {
    
    public static final int MAX_INPUTS = 3; // 2 registers + flags
    public static final int MAX_OUTPUTS = 2; // 1 register + flags
    
    public enum UopType {
        insn_LOAD(0), insn_STORE(1), insn_UBRANCH(2), insn_CBRANCH(3), insn_ALU(4), insn_MULTIPLY(5), insn_DIVIDE(6), insn_OTHER(7);
        
        public final int value;
        
        private UopType(int value) {
            this.value = value;
        }
        static public void printAll() {
            for (UopType type : UopType.values()) {
                System.out.format("Type %d: %s\n", type.value, type.toString());
            }
        }
        static public void printAllToFile(BufferedWriter out){
            for (UopType type : UopType.values()) {
                try{
                    out.write("Type " + type.value +": " + type.toString()+"\n");
                }
                catch (Exception e){//Catch exception if any
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }
    
    // See the documentation to understand what these variables mean.
    public long microOpCount;
    public long PC;
    public int inputRegs[] = new int[MAX_INPUTS]; // logical/architectural register names
    public int outputRegs[] = new int[MAX_OUTPUTS]; // logical/architectural register names
    public String conditionRegister;
    public String TNnotBranch;
    public String loadStore;
    public long immediate;
    public long addressForMemoryOp;
    public long fallthroughPC;
    public long targetPC;
    public String macroOperation;
    public String microOperation;
    public UopType type;
    
    //Adina's variables
    public String insnType;
    
    // INSN LATENCIES
    // how long does it take to execute each instruction
    
    static public int LOAD_LAT = 4;
    static public int INSN_LAT = 1;//ALU LOAD BRANCHES, OTHER
    static public int MULT_LAT = 3;
    static public int DIVI_LAT = 20;
    public int exec_latency = INSN_LAT;
    
    
    public Uop(String line, int flag_register) {
        
        String [] tokens = line.split("\\s+");
        
        microOpCount = Long.parseLong(tokens[0]);
        PC = Long.parseLong(tokens[1], 16);
        inputRegs[0] = Integer.parseInt(tokens[2]);
        inputRegs[1] = Integer.parseInt(tokens[3]);
        outputRegs[0] = Integer.parseInt(tokens[4]);
        conditionRegister = tokens[5]; // where the flags go
        insnType = tokens[13];//this is the Micro Opcode, I'm not sure if we need the Macrp Opcode instead?!
        
        if (conditionRegister.equals("R")) {
            // reads the flags --> another input!
            inputRegs[2] = flag_register;
        } else 
            inputRegs[2] = -1;
        
        if (conditionRegister.equals("W")) {
            // writes the flags --> another output!
            outputRegs[1] = flag_register;
        } else 
            outputRegs[1] = -1;
        
        TNnotBranch = tokens[6];
        loadStore = tokens[7];
        immediate = Long.parseLong(tokens[8]);
        addressForMemoryOp = Long.parseLong(tokens[9], 16);
        fallthroughPC = Long.parseLong(tokens[10], 16);
        targetPC = Long.parseLong(tokens[11], 16);
        macroOperation = tokens[12];
        microOperation = tokens[13];
        
        type = UopType.insn_OTHER;
        if(insnType.contains("ADD")||insnType.contains("SUB")||insnType.contains("AND")||(insnType.contains("OR")&&!insnType.contains("STORE")&&!insnType.contains("WORD"))){
            type = UopType.insn_ALU;
        }
        else{
            if(insnType.contains("MUL")){
                type = UopType.insn_MULTIPLY; 
                exec_latency = MULT_LAT;
            }
            else{
                if(insnType.contains("DIV")){
                    type = UopType.insn_DIVIDE;
                    exec_latency = DIVI_LAT;
                }
                else{
                    if (loadStore.equals("L")) {
                        type = UopType.insn_LOAD;
                        exec_latency = LOAD_LAT;
                    }
                    else if (loadStore.equals("S"))
                        type = UopType.insn_STORE;
                    else if (isBranch()) {
                        if (conditionRegister.equals("-"))
                            type = UopType.insn_UBRANCH;
                        if (conditionRegister.equals("R"))
                            type = UopType.insn_CBRANCH;
                    }
                }
            }
        }
        
    }
    
    /*
     * Helper functions!
     */    
    public boolean isBranch() { 
        return (targetPC != 0);     
    }
    public boolean isLoad() { 
        return (loadStore.equals("L"));     
    }
    public boolean isStore() { 
        return (loadStore.equals("S"));     
    }
    public boolean isMem() {
        return isLoad() || isStore();
    }
    
    // Print Methods!
    public void print_macro_micro_names() {
        System.out.format(" | %s %s \n", macroOperation, microOperation);
    }
    
}