import java.util.Arrays;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

public class SeatingSolver {
	
	private final int peopleCount;
	private final int tableCapacity;
	
	public SeatingSolver(int peopleCount, int tableCapacity) {
		this.peopleCount = peopleCount;
		this.tableCapacity = tableCapacity;
	}

	public int[][][] solve() {
		int minRounds = 1; // TODO
		
		int rounds = minRounds;
		while (true) {
			int[][][] result = solveWithRounds(rounds);
			if (result != null) {
				return result;
			}
			rounds++;
		}
	}

	public int[][][] solveWithRounds(int roundCount) {
		int tableCount = (int)Math.ceil((double)peopleCount / tableCapacity); 

		int offset = 1;
		
		Input input = new Input(offset, roundCount, tableCount, peopleCount);
		offset += input.count();

		Output output = new Output(offset, peopleCount);
		offset += output.count();

		Causing causing = new Causing(offset, roundCount, tableCount, peopleCount);
		offset += causing.count();
		
		ISolver solver = SolverFactory.newDefault();
		solver.newVar(offset);

		System.out.println("v: " + offset);
		
		try {

			int c = 0;
			
			// add table limits
			for (int round = 0; round < roundCount; round++) {
				for (int table = 0; table < tableCount; table++) {
					VecInt tableRoundLimit = new VecInt();
					for (int person = 0; person < peopleCount; person++) {
						tableRoundLimit.push(input.var(round, table, person));
					}
					solver.addAtMost(tableRoundLimit, tableCapacity);c++;
				}
			}
			
			// add person limits
			for (int round = 0; round < roundCount; round++) {
				for (int person = 0; person < peopleCount; person++) {
					VecInt personRoundLimit = new VecInt();
					for (int table = 0; table < tableCount; table++) {
						personRoundLimit.push(input.var(round, table, person));
					}
					solver.addExactly(personRoundLimit, 1);c++;
				}
			}
			
			// add constraints
			for (int person1 = 0; person1 < peopleCount; person1++) {
				for (int person2 = 0; person2 < peopleCount; person2++) {
					if (person1 != person2) {
						
						int assign = output.var(person1, person2);
						int[] wires = new int[roundCount * tableCount];
						
						for (int round = 0; round < roundCount; round++) {
							for (int table = 0; table < tableCount; table++) {
								int input1 = input.var(round, table, person1);
								int input2 = input.var(round, table, person2);
								int wire = causing.var(round, table, person1, person2);

								solver.addClause(new VecInt(new int[] {-wire, input1}));c++;
								solver.addClause(new VecInt(new int[] {-wire, input2}));c++;
								solver.addClause(new VecInt(new int[] {-input1, -input2, wire}));c++;

								solver.addClause(new VecInt(new int[] {-wire, assign}));c++;
								
								wires[(round * tableCount) + table] = wire;
							}
						}
						
						VecInt banAssignClause = new VecInt(wires);
						banAssignClause.push(-assign);
						solver.addClause(banAssignClause);c++;
						
						solver.addClause(new VecInt(new int[] {assign}));c++;
					}
				}
			}

			System.out.println("c: " + c);
			
			if (!solver.isSatisfiable()) {
				System.out.println("oops");
			    solver.reset();
				return null;
			}
			
		} catch (ContradictionException e) {
		    solver.reset();
			return null;
		} catch (TimeoutException e) {
		    solver.reset();
			return null;
		}

		int[][][] result = new int[roundCount][tableCount][tableCapacity];

		for (int round = 0; round < roundCount; round++) {
			for (int table = 0; table < tableCount; table++) {
				Arrays.fill(result[round][table], -1);
			}
		}
		
		for (int round = 0; round < roundCount; round++) {
			for (int table = 0; table < tableCount; table++) {
				int tableSeatIndex = 0; 
				for (int person = 0; person < peopleCount; person++) {
					int var = input.var(round, table, person);
					if (solver.model(var)) {
						result[round][table][tableSeatIndex++] = person;
					}
				}
			}
		}
		
	    solver.reset();
		return result;
	}
	
	private class Input {

		final int offset;
		final int roundCount;
		final int tableCount;
		final int peopleCount;
		
		Input(int offset, int roundCount, int tableCount, int peopleCount) {
			this.offset = offset;
			this.roundCount = roundCount;
			this.tableCount = tableCount;
			this.peopleCount = peopleCount;
		}
		
		public int var(int round, int table, int person) {
			return
				(round * tableCount * peopleCount) +
				(table * peopleCount) +
				person +
				offset
			;
		}

		public int count() {
			return roundCount * tableCount * peopleCount;
		}
		
	}

	private class Output {

		final int offset;
		final int peopleCount;
		
		Output(int offset, int peopleCount) {
			this.offset = offset;
			this.peopleCount = peopleCount;
		}

		public int var(int person1, int person2) {
			return
				(person1 * peopleCount) +
				person2 +
				offset
			;
		}
		
		public int count() {
			return peopleCount * peopleCount;
		}
		
	}

	private class Causing {

		final int offset;
		final int roundCount;
		final int tableCount;
		final int peopleCount;
		
		Causing(int offset, int roundCount, int tableCount, int peopleCount) {
			this.offset = offset;
			this.roundCount = roundCount;
			this.tableCount = tableCount;
			this.peopleCount = peopleCount;
		}
		
		public int var(int round, int table, int person1, int person2) {
			return
				(round * tableCount * peopleCount * peopleCount) +
				(table * peopleCount * peopleCount) +
				(person1 * peopleCount) +
				person2 +
				offset
			;
		}

		public int count() {
			return roundCount * tableCount * peopleCount * peopleCount;
		}
		
	}

}
