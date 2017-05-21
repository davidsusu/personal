package hu.webarticum.davidsusu.personal.sudokuthings;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
    	SudokuGenerator generator = new TestSudokuGenerator();
    	
    	Model model = generator.generate();

    	System.out.println("Initial model:");
    	System.out.println();
    	System.out.println(Arrays.deepToString(model.getData()).replace("],", "],\n"));

    	System.out.println();
    	System.out.println();
    	System.out.println();
    	
    	// TODO: solve...
    	
    	System.out.println("Solved model:");
    	System.out.println();
    	System.out.println(Arrays.deepToString(model.getData()).replace("],", "],\n"));
    }

}
