package uncas;

/* For Homework #4 you will consider a variety of cache configurations.
 * For each cache you configure & simulate, you will maintain the statistics
 * for that cache with an instance of THIS class.
 */
public class CacheStats {

	long total_accesses;
	long total_hits;
	double hit_rate;
	
	public CacheStats() {
		total_accesses = 0;
		total_hits = 0;
		hit_rate = 0.0; 
	}

	public long totalMisses() {
		return total_accesses - total_hits;
	}
	
    public void updateStat(boolean is_a_hit) {
    	if (is_a_hit)
    		total_hits++;
    	total_accesses++;
    }	
    
    // could do this in the previous method, but that's a lot of extra divides...
    public void calculateRates(){
    	hit_rate = total_hits/(double)total_accesses;
    }       
    
     public void print() {
    	System.out.format("Cache Hit Rate = %.2f%% (Mis-rate: %.2f%%)\n", 
    			hit_rate*100.0, (1-hit_rate)*100.0);
    }
}