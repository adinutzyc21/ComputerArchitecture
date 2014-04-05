package uncas;

import java.util.ArrayDeque;

public class ReorderBuffer {
    
	public ArrayDeque<ROB_entry> entries = null;
    public int max_entries = 0;
    
	public ReorderBuffer() {
		this.entries = new ArrayDeque<ROB_entry>();
	}
	
	public void setMaxEntries(int max_entries) {
		this.max_entries = max_entries;
	}
	
	public ROB_entry head() {
		return entries.getFirst();
	}

	// Remove from the Head
	public void dequeue() {
		entries.removeFirst();
	}
	
	// Add to the Tail
	public void enqueue(ROB_entry entry) {
		entries.addLast(entry);
	}
	
	public boolean isEmpty() {
		return entries.size() == 0;
	}

	public boolean isFull() {
		return entries.size() == max_entries;
	}
	
}

