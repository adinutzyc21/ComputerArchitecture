package uncas;

public class Cache {

	public int capacity; // in Bytes
    public int block_size; // in Bytes
    public int associativity;
    
    public int n_sets; 
    public int n_total_blockframes; 
    public int n_offset_bits;
    public int n_index_bits;
    public int n_tag_bits;
        
    public final int address_size = 32; // in bits
    
    // the tags are big numbers and need to be stored as longs
    public long[][] cache_tags = null;
    public boolean[][] dirty_bits = null;
    public int[] LRU_way= null;
    
    // use these fields to store the result of the most recent cache access
    public boolean hit_f = false;
    public boolean is_dirty_f = false;
    
	public Cache(int capacity, int block_size, int associativity) {
		this.capacity = capacity; // in B
		this.block_size = block_size; // in B
		this.associativity = associativity; // 1, 2, 3... etc.
		
	    n_total_blockframes = capacity / block_size;
	    n_sets = n_total_blockframes / associativity;
	    n_offset_bits = log2(block_size); 
	    n_index_bits = log2(n_sets);
	    n_tag_bits = address_size - n_index_bits - n_offset_bits;

	    // next create the cache tags
	    cache_tags = new long[n_sets][associativity];
		dirty_bits = new boolean[n_sets][associativity];
		LRU_way = new int[n_sets];
		
		// sets the cache tags all to 0 and all the dirty bits to false (cache starts clean)
		// and all the LRU bits to 0
		// this code is correct EXCEPT the for loops should not stop at [1][1]
		for (int i = 0; i < n_sets; i++) {
			for (int j = 0; j < associativity; j++) {
				cache_tags[i][j] = 0;
				dirty_bits[i][j] = false;
			}
			LRU_way[i] = 0;
		}
	  }
	
	public long extractTag(long addr) {
		// just shift it over, killing the index and the offset
		return addr >> (n_offset_bits + n_index_bits);
	}

	public int extractIndex(long addr) {
		int intAddr = (int)addr;
		// just shift it over to kill the offset bits
		int tmp = intAddr >> n_offset_bits; 
		
		int mask1 = (1 << n_index_bits)-1;
		// now mask it to get just the N index bits
		int tmp2 = tmp & mask1;
		return tmp2;
	}

	public long extractBlockAddress(long addr) {
		// just shift it over and then back again
		long blockAddr = addr >> (long)n_offset_bits; // shift over
		return blockAddr << (long)n_offset_bits; // shift back
	}
	
	/* this method takes a PC and a flag as to whether the access is a load or store
	 * functionality in no particular order: 
	 * 	(1) look up the address in the cache
	 * 	(2) update the cache_tags if necessary
	 * 	(3) set the hit_f flag accordingly
	 * 	(4) set the is_dirty_f flag accordingly
	 * 	(5) update the LRU_way field accordingly
	 * Remember to use "extract" helper functions above. They will make your life easier.
	 */
	public void access(long PC, boolean isLoad) {
		int index = extractIndex(PC);
		long tag = extractTag(PC);
		
		// look in each way
		for (int i = 0; i < associativity; i++) {
			if (cache_tags[index][i] == tag) {
				hit_f = true;
				updateLRU(index, i);
				// can't change from dirty to clean...
				if (!dirty_bits[index][i])
					dirty_bits[index][i] = !isLoad;
				return;
			}
		}
		
		hit_f = false;
		// THIS IS A MISS
		int replace_way = LRU_way[index];
		// enter new tag value
		cache_tags[index][replace_way] = tag;
		
		is_dirty_f = dirty_bits[index][replace_way];
		dirty_bits[index][replace_way] = !isLoad;
		updateLRU(index, replace_way);
	}

	/*
	 * If you think about it, LRU cannot be maintained with a single counter if there are
	 * more than 2 ways. So we'll just use an approximation: 
	 * 	If there is just one way, the LRU bit is always 0. 
	 * 	If there are two ways, the LRU bit is always the way you DIDN'T just touch. 
	 *  If there are more than 2 ways, the LRU bit is always 1 higher (w/wrap-around)
	 *  	than the way you just touched.
	 * For example, if there are 4 ways, and you touch way 0, then the new LRU should be 1.
	 * 		If you touch way 3, the new LRU should be 0.
	 * which_set: identifies the set in the cache we're talking about
	 * just_touched: identifies the way we just touched.
	 */
	public void updateLRU(int which_set, int just_touched) {
		// LRU remains 0
		if (associativity == 1)
			return;
		
		if (just_touched < (associativity-1))
			LRU_way[which_set] = just_touched + 1;
		else 
			LRU_way[which_set] = 0;
	}
	
	public void printConfig(){
		System.out.format("Cache size  = %dB. Each block = %dB.\nThis is a %d-way set associative cache.\n", 
				capacity, block_size, associativity);
		System.out.format("Tag = %d bits, Index = %d bits, Offset = %d bits\n",
				n_tag_bits, n_index_bits, n_offset_bits);
		System.out.format("There are %d sets total in the cache.\n"+
				"At this associativity, that means a total of %d block frames.\n"
				+"(Assuming a %d-bit address.)\n",
				n_sets, n_total_blockframes, address_size);
	}

	public void printCache() {
		for (int i = 0; i < n_sets; i++) {
			System.out.format("[S%d: ", i);		
			for (int j = 0; j < associativity; j++) {	
				System.out.format("{W%d: %s, %s} ", j, Long.toHexString(cache_tags[i][j]), dirty_bits[i][j] ? "D" : "C");		
			}
			System.out.format("LRU=%d] ", LRU_way[i]);	
		}
		System.out.format("\t| ");
	}
	
    /* 
     * You may find this useful.
     */
    static public int log2(double x) {
    	
    	return (int)(Math.log(x)/Math.log(2)+1e-10);
    	
    }

}
