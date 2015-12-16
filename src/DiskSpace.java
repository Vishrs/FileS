import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class DiskSpace {
	private int n_blocks;
	private int blocksize;
	private int blocksfree;
	private int charindex;
	private char[] chararray = new char[26];
	private Vector<Character> DiskSpaceMemory;
	private Hashtable<String, Character> files;
	private Hashtable<String, Integer> sizelookup;
	public DiskSpace(int blocks, int size, int free) {
		this.n_blocks = blocks;
		this.blocksize = size;
		this.blocksfree = free;
		files = new Hashtable<>();
		int q = 0;
		for (int i = 65; i <= 90; i++) {
			chararray[q] = (char) i;
			q++;
		}
		DiskSpaceMemory = new Vector<Character>();
		for (int i = 0; i < n_blocks; i++) {
			DiskSpaceMemory.addElement('.');
		}
		sizelookup = new Hashtable<>();
		charindex = 0;
	}
	
	public void printDir(DataOutputStream c) throws IOException {
		for (String f : files.keySet()) {
			c.writeChars(f + "\n");
		}
	}
	
	public boolean containsFile(String filename) {
		if (files.containsKey(filename))
			return true;
		else return false;
	}
	
	public char fileToChar(String filename) {
		return files.get(filename);
	}
	
	public int allocateFile(String filename, int block_s, int blocks) {
		int remaining_b = 0;
		int b = blocks;
		int blocksreq = 5000;
		
		for (Character c : DiskSpaceMemory) {
			if (c.equals('.')) { 	
				remaining_b++;
			}
		}
		
		if (blocks > remaining_b)
			return -1;
		int i = 0;
		int clusters = 0;
		
		for (Character c : DiskSpaceMemory) {
			if (c.equals('.') && b > 0) {
				DiskSpaceMemory.set(i, chararray[charindex]);
				//sizelookup.
				b--;
			}
			if (i != 127)
				if (b > 0 && DiskSpaceMemory.get(i).equals(chararray[charindex]) && 
						!DiskSpaceMemory.get(i + 1).equals('.'))
					clusters++;
			i++;
		}
		files.put(filename, chararray[charindex]);
		sizelookup.put(filename, block_s);
		charindex++;
		return clusters;
	}
	
	public int deallocateFile(String filename) {
		int count = 0;
		if (!files.containsKey(filename)) {
			return -1;	
		}
		
		for (int i = 0; i < 128; i++) {
			if (DiskSpaceMemory.get(i).equals(files.get(filename))) {
				DiskSpaceMemory.set(i, '.');
				count++;
			}
		}
		files.remove(filename);
		return count;
	}
	
	public long getSizeofFile(String filename) {
		return sizelookup.get(filename);
	}
	
	public void printDiskSpaceMemory() {
		System.out.println("Simulated Clustered Disk Space Allocation: ");
		for (int i = 0; i < 32; i++) {
			System.out.print("=");
		}
		System.out.println();
		int q = 0;
		for (int i = 0; i < n_blocks; i++) {
			System.out.print(DiskSpaceMemory.get(i));
			q++;
			if (q == 32) {
				System.out.println();
				q = 0;
			}
		}
		for (int i = 0; i < 32; i++) {
			System.out.print("=");
		}
		System.out.println();
	}
	
	
	
	
}
