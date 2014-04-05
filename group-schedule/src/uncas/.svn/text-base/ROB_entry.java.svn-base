package uncas;

public class ROB_entry {

	public Uop theUop;
    public int sequenceNumber;
    public boolean hasIssued_f;
    public boolean mispredictedBranch_f;
    
    // these two entries are coupled. registerReady[0] tells you whether physicalInputRegs[0] is ready.
    public int physicalInputRegs[] = new int[Uop.MAX_INPUTS]; // physical register names
    public int physicalOutputRegs[] = new int[Uop.MAX_OUTPUTS]; // physical register names

    public int overwrittenRegs[] = new int[Uop.MAX_OUTPUTS]; // when this ROB entry commits, free these!
    
    public int fetchCycle = -1;
    public int issueCycle = -1;
    public int doneCycle = -1;
    public int commitCycle = -1;
    
    // Hila - adding score to ROB entry
    public int score = 0;
    
	public ROB_entry(Uop theUop, int sequence_number, int fetchCycle, boolean isMispredicted) {
		this.theUop = theUop; 
		this.sequenceNumber = sequence_number; 
		this.hasIssued_f = false;
		this.mispredictedBranch_f = isMispredicted;
		
		this.fetchCycle = fetchCycle;
		
	}
	
	public boolean isDone(int currentCycle) {
			return hasIssued_f && (doneCycle <= currentCycle);
	}
	
	// Print methods!
	
	public void printCycles() {
		System.out.format("%d: %d %d %d %d", sequenceNumber, fetchCycle, issueCycle, doneCycle, commitCycle);
	}
	
    /*
     * prints the input mappings of the instruction
     */
    public void printInputRegisters() {
    	for (int i = 0; i < Uop.MAX_INPUTS; i++) {
    		if (theUop.inputRegs[i] != -1) {
    			System.out.format(", r%d -> p%d", theUop.inputRegs[i], physicalInputRegs[i]);
    		}		
    	}			
    }
    
    /*
     * prints the output mappings of the instruction
     */
	public void printOutputRegisters() {
		for (int i = 0; i < Uop.MAX_OUTPUTS; i++) {
			if (theUop.outputRegs[i] != -1) {
				System.out.format(", r%d -> p%d [p%d]", theUop.outputRegs[i], physicalOutputRegs[i], overwrittenRegs[i]);
			}		
		}	
	}
	
	
}
