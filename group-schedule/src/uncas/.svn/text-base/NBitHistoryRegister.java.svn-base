package uncas;

public class NBitHistoryRegister  {
    
    public int register;
    public int nBits;
    
    public NBitHistoryRegister(int nbits) {
        register = 0; // begin all 0's
        nBits = nbits;
    }
    
    // completed/overloaded method
    public void update(boolean isTaken) {
        
        register = register << 1; // shift over 
        register = register & ((int)Math.pow(2.0,nBits)-1); // keep it nBits in length
        if (isTaken) // add in lowest bit
            register++;
    }
    
    public void print() {
        // useful for the debug printing
        
        for (int i = nBits-1; i >= 0 ; i--) {
            int biggestBit = (int)Math.pow(2.0,i);
            if ((register & biggestBit) >0)
                System.out.format("T");
            else
                System.out.format("N");
        }
    }
}
