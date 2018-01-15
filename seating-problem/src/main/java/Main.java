
public class Main {
	
    public static void main(String[] args) {
    	int peopleCount = 15;
    	int tableCapacity = 5;
    	
        int[][][] result = new SeatingSolver(peopleCount, tableCapacity).solve();
        
        visualize(peopleCount, result);
    }
    
    private static void visualize(int peopleCount, int[][][] result) {
    	System.out.println(result.length + " körből megoldva:");
    	
    	for (int[][] roundData: result) {
    		System.out.println();
    		
    		for (int[] tableData: roundData) {
    			for (int person: tableData) {
    				if (person != -1) {
    					System.out.print(person + "; ");
    				}
    			}
        		System.out.println();
    		}
    	}
    }
    
}
