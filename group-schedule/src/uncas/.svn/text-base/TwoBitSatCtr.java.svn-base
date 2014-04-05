package uncas;

public class TwoBitSatCtr {
    
    public int counter;
    
    public TwoBitSatCtr() {
        counter = 0; // Strongly Not Taken initially
    }
    
    public void inc() {
        if (counter < 3) {
            counter++;
        }
    }
    
    public void dec() {
        if (counter > 0) {
            counter--;
        }
    }
    
    public char getChar() {
        switch (counter) {
            case 0: return 'N';
            case 1: return 'n';
            case 2: return 't';
        }
        return 'T';
    }
    
    public char getPredictionChar() {
        if (counter >=2)
            return 'T';
        return 'N';
    }
    
    public void train(boolean wasTaken) {
        if (wasTaken)
            inc();
        else
            dec();
    }
    
    public boolean predictTaken() {
        return (counter >=2);
    }
}
