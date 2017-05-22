package hu.webarticum.davidsusu.personal.sudokuthings;

public class TestSudokuGenerator implements SudokuGenerator {

	@Override
	public Model generate() {
		Model model = new Model(3, 3);
		
		// XXX / TODO
		model.set(0, 0, 3);
		model.set(0, 6, 2);
		model.set(4, 4, 7);
		model.set(5, 5, 9);
		
		return model;
	}

}
