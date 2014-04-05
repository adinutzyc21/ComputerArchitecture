package uncas;

import uncas.NBitHistoryRegister;
import uncas.TwoBitSatCtr;
import uncas.Uop;
import uncas.Uop.UopType;
import uncas.SimAssert;
import uncas.Heuristic;
import uncas.Heuristic.HeuristicType;

import java.util.*;
import java.lang.String;
import java.util.zip.GZIPInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Iterator;

public class PerfSim
{        
    // don't modify this!
    public BufferedReader traceReader = null; 
    
    // things you can have the simulator print out
    public boolean printEachUop_f = false;
    public boolean printStats_f = true;
    
    public int uopLimit = 100;
    public boolean debug_f = false;
    
    // BRANCH PREDICTION, Gshare only here
    public int bpredPenalty = 5; // 5 cycle penalty for a mis-predicted branch
    public boolean perfect_bpred_f = true; // must be set to false in order to turn on the GShare Branch Predictor
    public PredictorStats Gshare_Stats = new PredictorStats("Gshare", "with history bits");
    public TwoBitSatCtr[] gshare_table = null;
    public int gshare_n_entries = 0;
    public NBitHistoryRegister gshare_history_register;
    
    // Cache
    public int cacheSz = 4096;
    public boolean perfectCache_f = true; // must be set to false in order to turn on the cache
    public int cacheMissPenalty = 20; // 20 cycle penalty for a cache miss
    public CacheStats myCacheStats = null;
    public Cache myCache = null;
    public boolean lastLoadMissed = false;
    
    // Program Stats, total number COMMITTED
    public int totalUops = 0;
    public int totalMops = 0;
    public int nROBStalls = 0;
    public int nPregStalls = 0;
    public double IPC = 0.0;
    
    // This is used to recognize that your processor is FROZEN
    public int lastCommitCycle = -1;
    public int n_fetched = 0;
    
    // Register Renaming Structures
    public final int NUM_ARCH_REGISTERS = 50;
    public int MapTable[] = null;
    public ArrayDeque<Integer> FreeList = null;
    
    // this is not public b/c you should not change this value after the constructor is called
    // the constructor uses this number to create the scoreboard, so you need to tell the constructor
    // how many physical registers there are. after that, it's too late to change it.
    protected int num_physical_registers = NUM_ARCH_REGISTERS; // you will want to use something bigger than this
    
    public ReorderBuffer ROB = null;
    public int[] scoreboard = null;    
    
    public int machineWidth = 1;
    public int currCycle = 0;
    public int fetchReady = 0; 
    
    static public Heuristic heuristic; // heuristic params
    
    /*changing these would run different experiments*/
    
    public int latencyMultiplier = 3;//also tested 1 and 2
    public int heurType = 1;//1 is Basic Score, 2 is Basic Sore + Sequence Number, 3 is Basic Score = Latency
    
    public int scoreIncrForSNR = 1;//used to calculate the sequence number
    public int LOAD_SCORE = 2;
    public int MULTIPLY_SCORE = 1;
    public int DIVIDE_SCORE = 3;
    public int BRANCH_SCORE = 3;
    public int MISPREDICTED_BRANCH_SCORE = 10;
    public int PRIORITY_LOADS_SCORE = 20;
    public int MISSED_LOADS_SCORE = 20;
    
    public PerfSim(String traceFile, int num_physical_registers) throws IOException {
        // ugly, but JAVA IO is not the point
        traceReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(traceFile))));
        
        MapTable = new int[NUM_ARCH_REGISTERS];
        for (int i = 0; i < NUM_ARCH_REGISTERS; i++) {
            MapTable[i] = i;
        }
        
        FreeList = new ArrayDeque<Integer>();
        for (int i = (num_physical_registers - 1); i > (NUM_ARCH_REGISTERS -1); i--) {
            FreeList.addFirst(new Integer(i));
        }
        
        this.num_physical_registers = num_physical_registers;
        // must be at least this large
        SimAssert.check((num_physical_registers >= NUM_ARCH_REGISTERS), "Num pRegs must be >= num arch regs", 0);
        
        scoreboard = new int[num_physical_registers]; 
        // initialize to all zeroes
        for (int i = 0; i < num_physical_registers; i++) {
            scoreboard[i] = 0;
        }
        
        ROB = new ReorderBuffer();
        
        // initialize the Branch Predictor, which is on by default
        configureGshare(16, 16); // 2^16 counters, 16 history bits = 16KB branch predictor by default, feel free to change
        
        // initialize the Cache, which is on by default
        configureCache(cacheSz, 64, 2); //capacity, block_size, associativity
        
    }
    
    public void commit() {
        
        // if you haven't committed anything in 10,000 cycles, simulator is probably deadlocked --> kill
        SimAssert.check(((currCycle - lastCommitCycle) < 10000), "Deadlock! no commits in 10K cycles", currCycle);
        
        for (int i = 0; i < machineWidth; i++) {
            if (ROB.isEmpty()) // nothing to commit. bye!
                break;
            ROB_entry head = ROB.head(); 
            Uop headUop = head.theUop;
            if (head.doneCycle <= currCycle && (head.doneCycle != -1)) {
                head.commitCycle = currCycle;
                if (debug_f) {
                    head.printCycles();
                    head.printInputRegisters();
                    head.printOutputRegisters();
                    headUop.print_macro_micro_names();
                }
                
                // at commit, free the overwritten register
                for (int out = 0; out < Uop.MAX_OUTPUTS; out++) {
                    if (headUop.outputRegs[out] != -1) {
                        FreeList.addLast(head.overwrittenRegs[out]);
                    }
                }
                
                ROB.dequeue();
                totalUops++;
                lastCommitCycle = currCycle;
            }
        }
    }
    
    public void issue() {// use different issue functions depending on heuristic type
        switch (heuristic.type) {
            case heuristic_AGE: // age: oldest instructions first
                issue_Age();
                break;
            case heuristic_LATENCY: //latency: instructions that take more to run first
                issue_Latency();
                break;
            default:
                issue_Score(); // default is the score function that computes different scores, see below
                break;
        }
    }
    
    /* ###########################################################
     * =================== The ISSUE FUNCTIONS ===================
     * ###########################################################
     */
    public void issue_Age() { // This is the original function - just changed its name to issue_Age: issue oldest ready instructions first
        int count = 0; //count the number of instructions to issue in current cycle
        ROB_entry[] currentCycleIssuedInstr=new ROB_entry[machineWidth];
        
        for (Iterator<ROB_entry> itr = ROB.entries.iterator(); itr.hasNext(); )  {
            ROB_entry currEntry = itr.next();
            Uop currUop = currEntry.theUop;
            
            if (!currEntry.hasIssued_f && isUopReady(currEntry, currentCycleIssuedInstr,count)) { // make sure inputs are ready, etc.
                currEntry.hasIssued_f = true;
                currEntry.issueCycle = currCycle;
                currEntry.doneCycle = currCycle + currUop.exec_latency;
                currentCycleIssuedInstr[count] = currEntry;
                
                if (!perfectCache_f && currUop.isMem()) {
                    myCache.access(currUop.addressForMemoryOp, currUop.isLoad());
                    myCacheStats.updateStat(myCache.hit_f);
                    
                    if (currUop.isLoad() && !myCache.hit_f){// load misses cost more
                        currEntry.doneCycle = currCycle + cacheMissPenalty;
                    }
                }
                
                if (currEntry.mispredictedBranch_f) {
                    SimAssert.check (fetchReady == -1, "should be stalling fetch until mis-predicted branch executes", currCycle); 
                    fetchReady = currEntry.theUop.exec_latency + bpredPenalty;  // The mis-prediction restart penalty
                }
                
                updateScoreboard(currEntry);
                
                count = count + 1;
                if (count == machineWidth) {
                    break;  // stop looking for instructions to execute
                }
            }
        }
    }
    
    public void issue_Latency() { // issue instructions that take longer to execute first (special case of score)
        int count = 0; //count the number of instructions in the ROB
        int number=0; //count the number of instructions to issue in current cycle
        ROB_entry[] currentCycleIssuedInstr = new ROB_entry[machineWidth];
        ROB_entry[] currROBEntry = new ROB_entry[ROB.max_entries];//make a new ROB so that you don't alter the original one
        
        for (Iterator<ROB_entry> itr = ROB.entries.iterator(); itr.hasNext(); ) {//copy the ROB over and count the instructions
            ROB_entry currEntry = itr.next();
            currROBEntry[count] = currEntry;
            count++;
        }
        
        Integer[][] finalArr = new Integer[count][2];//first column is order of instructions, second is what we're ordering by
        for(int i=0;i<count;i++){
            finalArr[i][0] = i;//an index into the unordered array
            finalArr[i][1] = currROBEntry[i].theUop.exec_latency;//the second column here should be the score
        }
        
        if(count != 0) {//if we have instructions in the ROB
            finalArr = mysort(finalArr);
            
            for(int k=0; k<count; k++) {
                if (!currROBEntry[finalArr[k][0]].hasIssued_f && isUopReady(currROBEntry[finalArr[k][0]],currentCycleIssuedInstr,number)) {//make sure inputs are ready, etc.
                    currROBEntry[finalArr[k][0]].hasIssued_f = true;
                    currROBEntry[finalArr[k][0]].issueCycle = currCycle;
                    currROBEntry[finalArr[k][0]].doneCycle = currCycle + currROBEntry[finalArr[k][0]].theUop.exec_latency;
                    currentCycleIssuedInstr[number] = currROBEntry[finalArr[k][0]];
                    
                    if (!perfectCache_f && currROBEntry[finalArr[k][0]].theUop.isMem()) {
                        myCache.access(currROBEntry[finalArr[k][0]].theUop.addressForMemoryOp, currROBEntry[finalArr[k][0]].theUop.isLoad());
                        myCacheStats.updateStat(myCache.hit_f);
                        
                        if (currROBEntry[finalArr[k][0]].theUop.isLoad() && !myCache.hit_f) {// load misses cost more
                            currROBEntry[finalArr[k][0]].doneCycle = currCycle + cacheMissPenalty;
                        }
                    }
                    
                    if (currROBEntry[finalArr[k][0]].mispredictedBranch_f) {
                        SimAssert.check (fetchReady == -1, "should be stalling fetch until mis-predicted branch executes", currCycle);
                        fetchReady = currROBEntry[finalArr[k][0]].theUop.exec_latency + bpredPenalty;  // The mis-prediction restart penalty
                    }
                    
                    updateScoreboard(currROBEntry[finalArr[k][0]]);
                    number++;
                    
                    if(number==machineWidth)
                        break;
                }
            }
        }
    }
    
    public void issue_Score() {//use a score function
        int count = 0; //count the number of instructions in the ROB
        int number = 0; //count the number of instructions to issue in current cycle
        ROB_entry[] currentCycleIssuedInstr=new ROB_entry[machineWidth];
        ROB_entry[] currROBEntry=new ROB_entry[ROB.max_entries];//make a new ROB so that you don't alter the original one
        
        for (Iterator<ROB_entry> itr = ROB.entries.iterator(); itr.hasNext(); )  {//simulate the entry age by counting which entry in the ordered ROB it is
            ROB_entry currEntry = itr.next();
            
            
            //###################
            if(heurType==2 || heuristic.type == HeuristicType.heuristic_LATENCY_SNR)//if we care about the sequence number
                currEntry.score += scoreIncrForSNR;// increase score to simulate the sequence number
            //###################
            
            currROBEntry[count]=currEntry;
            
            count++;
        }
        
        Integer[][] finalArr = new Integer[count][2];//first column is order of instructions, second is what we're ordering by
        for(int i=0;i<count;i++){
            finalArr[i][0] = i;//an index into the unordered array
            finalArr[i][1] = currROBEntry[i].score;//the second column here should be the score
        }
        
        if(count != 0) { //if we have instructions in the ROB
            
            finalArr = mysort(finalArr);//sort the array
            
            for(int k=0; k<count; k++) {
                if (!currROBEntry[finalArr[k][0]].hasIssued_f && isUopReady(currROBEntry[finalArr[k][0]],currentCycleIssuedInstr,number)) {//make sure inputs are ready, etc.
                    currROBEntry[finalArr[k][0]].hasIssued_f = true;
                    currROBEntry[finalArr[k][0]].issueCycle = currCycle;
                    currROBEntry[finalArr[k][0]].doneCycle = currCycle + currROBEntry[finalArr[k][0]].theUop.exec_latency;
                    currentCycleIssuedInstr[number] = currROBEntry[finalArr[k][0]];
                    
                    if (!perfectCache_f && currROBEntry[finalArr[k][0]].theUop.isMem()) {
                        myCache.access(currROBEntry[finalArr[k][0]].theUop.addressForMemoryOp, currROBEntry[finalArr[k][0]].theUop.isLoad());
                        myCacheStats.updateStat(myCache.hit_f);
                        
                        if (currROBEntry[finalArr[k][0]].theUop.isLoad()) {// load misses cost more
                            if(!myCache.hit_f) {//if we miss in the cache
                                currROBEntry[finalArr[k][0]].doneCycle = currCycle + cacheMissPenalty;
                                lastLoadMissed = true;
                            }
                            else {//if we don't miss in the cache
                                lastLoadMissed = false;
                            }
                        }
                        
                    }
                    
                    if (currROBEntry[finalArr[k][0]].mispredictedBranch_f) {
                        SimAssert.check (fetchReady == -1, "should be stalling fetch until mis-predicted branch executes", currCycle);
                        fetchReady = currROBEntry[finalArr[k][0]].theUop.exec_latency + bpredPenalty;  // The mis-prediction restart penalty
                    }
                    
                    updateScoreboard(currROBEntry[finalArr[k][0]]);
                    number++;
                    
                    if(number==machineWidth)
                        break;
                }
            }
        }
    }
    
    public boolean isUopReady(ROB_entry currEntry,ROB_entry[] currentCycleIssuedInstr, int numOfIssuedInst) {
        Uop currUop = currEntry.theUop;
        
        for (int i = 0; i < Uop.MAX_INPUTS; i++) {
            int logicalReg = currUop.inputRegs[i];
            
            // check only valid inputs
            if (logicalReg != -1) {
                int physicalReg = currEntry.physicalInputRegs[i];
                if (scoreboard[physicalReg] != 0)
                    return false;
            }
        }
        
        // a load can issue only when all prior stores WITH THE SAME ADDRESS have issued
        // this is an optimistic simulation mode that assumes we can predict which loads
        // depend on which stores. it's modeled with a bit of cheating... at this stage in
        // the pipeline we normally wouldn't actually know the Address for the Memory operation
        if (currUop.isLoad()) {
            for (Iterator<ROB_entry> itr = ROB.entries.iterator(); itr.hasNext(); )  {
                ROB_entry otherEntry = itr.next();
                Uop otherUop = otherEntry.theUop;
                
                if (otherUop.isStore() && 
                    (otherEntry.sequenceNumber < currEntry.sequenceNumber) &&
                    !otherEntry.isDone(currCycle) &&
                    (currUop.addressForMemoryOp == otherUop.addressForMemoryOp)) {
                    return false;
                }
            }
        }
        
        // Restrictions: you can only issue 1 load or store, 1 multiply, 1 divide, 1 branch at a time
        if (numOfIssuedInst != 0) {// if it's the first issued instruction in this cycle it's ok
            if(currUop.type == UopType.insn_LOAD  || currUop.type == UopType.insn_STORE || currUop.type == UopType.insn_MULTIPLY ||
               currUop.type == UopType.insn_DIVIDE || currUop.type == UopType.insn_UBRANCH || currUop.type == UopType.insn_CBRANCH) {
                UopType restrictAdditionalType = currUop.type;
                    
                for(int j = 0; j < numOfIssuedInst; ++j) {
                    if(restrictAdditionalType == UopType.insn_UBRANCH || restrictAdditionalType == UopType.insn_CBRANCH){//no more than one branch at a time
                        if (currentCycleIssuedInstr[j].theUop.type == UopType.insn_UBRANCH || currentCycleIssuedInstr[j].theUop.type == UopType.insn_CBRANCH)
                            return false;
                    }
                    else{
                        if(restrictAdditionalType == UopType.insn_LOAD || restrictAdditionalType == UopType.insn_STORE){//no more than one load or store at a time
                            if (currentCycleIssuedInstr[j].theUop.type == UopType.insn_LOAD || currentCycleIssuedInstr[j].theUop.type == UopType.insn_STORE)
                                return false;
                        }
                        else{
                            if (currentCycleIssuedInstr[j].theUop.type == restrictAdditionalType)
                                return false;
                        }
                    }
                }
            }
        }
        
        return true;
    }
    
    public void updateScoreboard(ROB_entry currEntry) {
        for (int i = 0; i < Uop.MAX_OUTPUTS; i++) {
            int logicalReg = currEntry.theUop.outputRegs[i];
            if (logicalReg != -1) {
                int physicalReg = currEntry.physicalOutputRegs[i];
                scoreboard[physicalReg] = currEntry.theUop.exec_latency;
            }
        }
    }
    
    /* ##################################################
     * =================== The SCORES ===================
     * ##################################################
     */
    public void calculateBasicScore(ROB_entry entry) { // the baseline score, based only on instruction type (+mispredicted branch)
        if (entry.theUop.type == UopType.insn_CBRANCH){ // score for conditional branch
            entry.score += BRANCH_SCORE;
            if(heuristic.type != HeuristicType.heuristic_BASE_SCORE){//if we're doing the basic score we don't have the misprediction predictor
                if (entry.mispredictedBranch_f) {// check if need to add score for mispredicted branch
                    entry.score += MISPREDICTED_BRANCH_SCORE;
                }
            }
        }
        else if (entry.theUop.type == UopType.insn_UBRANCH) // score for unconditional branch
            entry.score += BRANCH_SCORE;
        else if (entry.theUop.type == UopType.insn_LOAD) // score for load
            entry.score += LOAD_SCORE;
        else if (entry.theUop.type == UopType.insn_DIVIDE) // score for divide
            entry.score += DIVIDE_SCORE;
        else if (entry.theUop.type == UopType.insn_MULTIPLY) // score for multiply
            entry.score += MULTIPLY_SCORE;
    }
    
    public void calculateBasicScoreSpecial(ROB_entry entry) { //latency except for missed branches (unless base score)
        entry.score = entry.theUop.exec_latency;
        if (entry.theUop.type == UopType.insn_CBRANCH){ // score for conditional branch
            if(heuristic.type != HeuristicType.heuristic_BASE_SCORE){//if we're doing the basic score we don't have the misprediction predictor
                if (entry.mispredictedBranch_f) {// check if need to add score for mispredicted branch
                    entry.score += MISPREDICTED_BRANCH_SCORE;
                }
            }
        }
        else if(heuristic.type == HeuristicType.heuristic_LOADS_FIRST){
            entry.score += PRIORITY_LOADS_SCORE;
        }
        
        else if(heuristic.type == HeuristicType.heuristic_LOAD_MISSED ){
            if (entry.theUop.type == UopType.insn_LOAD && lastLoadMissed) {
                entry.score += MISSED_LOADS_SCORE;
            }
        }
        
        else if(heuristic.type == HeuristicType.heuristic_LOADS_FIRST_MISSED ){
            entry.score += PRIORITY_LOADS_SCORE;
            if (entry.theUop.type == UopType.insn_LOAD && lastLoadMissed) {
                entry.score += MISSED_LOADS_SCORE;
            }
        }
    }
    
    /*This is how we assign the initial score*/
    public void calculateInitialScore(ROB_entry entry) {//calculate the score based on which heuristic we use for the score function
        if(heuristic.type == HeuristicType.heuristic_LATENCY_SNR ) {//sum of sequence number and multiplier * latency
            entry.score += latencyMultiplier * entry.theUop.exec_latency;
        }
        
        else if(heurType != 3){//the latency is not the base score
            if(heuristic.type == HeuristicType.heuristic_LOADS_FIRST) {//give special score to loads (bigger than usual)
                LOAD_SCORE = PRIORITY_LOADS_SCORE;
                calculateBasicScore(entry);//on top of the basic score
            }
            
            else if(heuristic.type == HeuristicType.heuristic_LOAD_MISSED ){//special score to loads plus extra points if previous load missed
                if (entry.theUop.type == UopType.insn_LOAD && lastLoadMissed) {
                    entry.score += MISSED_LOADS_SCORE;
                }
                calculateBasicScore(entry);//on top of the basic score
            }
            
            else if(heuristic.type == HeuristicType.heuristic_LOADS_FIRST_MISSED ){//special score to loads plus extra points if previous load missed
                LOAD_SCORE = PRIORITY_LOADS_SCORE;//also assign the priority score
                if (entry.theUop.type == UopType.insn_LOAD && lastLoadMissed) {
                    entry.score += MISSED_LOADS_SCORE;
                }
                calculateBasicScore(entry);//on top of the basic score
            }
            else //default score is the instruction type score
                calculateBasicScore(entry);
        }
        else
            calculateBasicScoreSpecial(entry);//for latenccy
    }
    
    /*
     * Fetches instructions, renames them, adds them to the ROB
     */
    public boolean fetch_and_rename() throws IOException {
        Uop currUop = null;
        String line = "";    
        
        for (int i = 0; i < machineWidth; i++) {
            
            // the following lines delay fetch when a branch was mis-predicted
            if (fetchReady > 0) {
                fetchReady--;
                break; 
            } 
            if (fetchReady == -1)
                break;
            SimAssert.check(fetchReady == 0, "Can only fetch when fetchReady = 0", currCycle);
            
            // is there room for these instructions in the ROB?
            if (ROB.isFull()) {
                nROBStalls++;
                break;
            }
            
            line = traceReader.readLine();
            if (line == null)
            {
                //break;
                return false;// Return false when there are no new lines to read
            }
            n_fetched = n_fetched + 1;
            
            currUop = new Uop(line, NUM_ARCH_REGISTERS-1);
            
            // prints the insn
            if (printEachUop_f)
                System.out.format("%s\n", line);
            
            boolean isMispredicted_f = false;
            if (!perfect_bpred_f && (currUop.type == UopType.insn_CBRANCH)) {
                boolean isTaken = currUop.TNnotBranch.equals("T");
                isMispredicted_f = !update_gshare_table(isTaken, currUop);
                if (isMispredicted_f)
                    fetchReady = -1; // Suspend fetch
            }
            
            ROB_entry addToROB = new ROB_entry(currUop, n_fetched, currCycle, isMispredicted_f);
            
            calculateInitialScore(addToROB);//calculate the score
            
            ROB.enqueue(addToROB);
            
            // record input mappings
            for (int in = 0; in < Uop.MAX_INPUTS; in++) {
                if (currUop.inputRegs[in] != -1)
                    addToROB.physicalInputRegs[in] = MapTable[currUop.inputRegs[in]]; 
            }
            
            // record the overwritten register, get new output register, update the Map Table
            for (int out = 0; out < Uop.MAX_OUTPUTS; out++) {
                if (currUop.outputRegs[out] != -1) {
                    addToROB.overwrittenRegs[out] = MapTable[currUop.outputRegs[out]];
                    SimAssert.check(!FreeList.isEmpty(), "you've run out of Pregs: you've got a bug OR you need more physical registers", currCycle);
                    int new_output_reg = FreeList.removeFirst();
                    MapTable[currUop.outputRegs[out]] = new_output_reg;
                    addToROB.physicalOutputRegs[out] =  new_output_reg;
                    
                }
            }
            
            // set register "not ready": set corresponding scoreboard entry to -1
            for (int out = 0; out < Uop.MAX_OUTPUTS; out++) {
                if (currUop.outputRegs[out] != -1) {
                    int physicalReg = MapTable[currUop.outputRegs[out]];
                    scoreboard[physicalReg] = -1;
                }
            }
        }
        return true;
    }
    /*
     * You should be able to complete Part 2 without modifying this method.
     * Simply completing the methods that this method calls should be sufficient.
     * 
     */
    public void simulate() throws IOException{
        
        System.out.format("### %s ###\n",heuristic.type.toString());
        boolean moreLinesToRead = true;
        
        if (debug_f)
            print_debug_header();   
        
        while (true) {
            commit();
            
            issue();
            
            moreLinesToRead = fetch_and_rename();
            
            currCycle = currCycle + 1;
            decrementScoreboard();
            
            if (totalUops >= uopLimit)
            {
                System.out.format("Reached insn limit of %d @ cycle %d. Ending Simulation...\n", uopLimit, currCycle);
                break;
            }
            else if (!moreLinesToRead && ROB.isEmpty())
            {
                System.out.format("Reached to the end of trace totalUops %d @ cycle %d. Ending Simulation...\n", totalUops, currCycle);
                break;
            }
            
        }
        
        endStats();
        
        if (printStats_f)
            printProgramStats();//print the program stats to help debugging
        
        System.out.format("##############################\n");
    }
    
    // stolen from a previous homework assignment.
    public void decrementScoreboard(){
        for (int i = 0; i < num_physical_registers; i++) {
            if (scoreboard[i] != 0)
                scoreboard[i]--;
        }
    }
    
    /*
     * When the simulation is complete, call this method to finish any final computations
     * that need to be completed. 
     */
    public void endStats()
    {
        //IPC = (double)uopLimit/currCycle;
        IPC = (double)totalUops/currCycle;// Calculate IPC using totalUops
        
        if (!perfect_bpred_f)
            Gshare_Stats.calculateRate();
        
        if (!perfectCache_f)
            myCacheStats.calculateRates();
        
    }
    // ------ CACHE METHODS GO HERE --------
    public void configureCache(int capacity, int block_size, int associativity) {
        //perfectCache_f = false;
        myCacheStats = new CacheStats();
        myCache = new Cache(capacity, block_size, associativity);
    }
    
    // ------ BRANCH PREDICTION METHODS GO HERE --------
    public void configureGshare(int nbits, int history_len) {
        //perfect_bpred_f = false;
        gshare_n_entries = (int)Math.pow(2.0,nbits);
        gshare_table = new TwoBitSatCtr[gshare_n_entries];
        for (int i = 0; i < gshare_n_entries; i++) {
            gshare_table[i] = new TwoBitSatCtr();
        }
        gshare_history_register = new NBitHistoryRegister(history_len);
    }
    
    public boolean update_gshare_table(boolean isTaken, Uop currUop) {
        int PC_xor_history = (int)currUop.PC ^ gshare_history_register.register;
        int which_entry = PC_xor_history % gshare_n_entries;
        boolean isCorrect = (isTaken == gshare_table[which_entry].predictTaken());
        Gshare_Stats.updateStat(isCorrect);
        
        gshare_table[which_entry].train(isTaken);
        gshare_history_register.update(isTaken);
        
        return isCorrect;
    }
    
    // Helper function to do the ordering
    // code from http://stackoverflow.com/questions/7937134/sorting-a-2d-integer-array-based-on-a-column
    public static Integer[][] mysort(Integer[][] ar) {        
        Arrays.sort(ar, new Comparator<Integer[]>() {
            @Override
            public int compare(Integer[] int1, Integer[] int2) {
                Integer numOfKeys1 = int1[1];
                Integer numOfKeys2 = int2[1];
                return numOfKeys2.compareTo(numOfKeys1);
            }
        });
        return ar;
    }
    
    // -------PRINT METHODS BEGIN HERE --------
    
    /*
     * Prints out the hit/miss rate of the cache in use
     */
    public void printProgramStats() {
        
        System.out.format("Processed %d trace lines.\n", totalUops);
        System.out.format("Num Uops (micro-ops): %d\n", totalUops);
        System.out.format("Num times the ROB stalled b/c it was full: %d\n", nROBStalls);
        System.out.format("IPC: %.3f\n", IPC);
        
        PrintInputParams();
        
        if (!perfect_bpred_f)
            Gshare_Stats.print();
        
        if (!perfectCache_f)
            myCacheStats.print();
    }
    public void PrintInputParams(){ // also print the parameters we're using
        System.out.format("Input Params: ");
        System.out.format("LOAD_SCORE = %d, ",LOAD_SCORE);
        System.out.format("MULTIPLY_SCORE = %d, ",MULTIPLY_SCORE);
        System.out.format("DIVIDE_SCORE = %d, ",DIVIDE_SCORE);
        System.out.format("BRANCH_SCORE = %d, ",BRANCH_SCORE);
        System.out.format("MISPREDICTED_BRANCH_SCORE = %d, ",MISPREDICTED_BRANCH_SCORE);
        System.out.format("scoreIncrForSNR = %d, ",scoreIncrForSNR);
        System.out.format("PRIORITY_LOADS_SCORE = %d, ",PRIORITY_LOADS_SCORE);
        System.out.format("MISSED_LOADS_SCORE = %d, \n",MISSED_LOADS_SCORE);
        
    }
    /*
     * for Debug mode, you'll want to print headers to know what each column means
     */
    
    public void print_debug_header() {
        System.out.format("#: F I D C \t Reg Mappings [freeMe] | Mop Uop\n");
    }
    
}
