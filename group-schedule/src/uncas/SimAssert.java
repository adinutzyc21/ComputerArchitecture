package uncas;

public class SimAssert {
    
    static public void check(boolean test, String errorMsg, int cycle) {
        if (!test) {
            System.out.format("ASSERTION FAILURE @ cycle %d: %s ", cycle, errorMsg);
            System.exit(0);
        }
    }
}