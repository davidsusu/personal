package hu.webarticum.davidsusu.personal.evolutionvssatsolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;
import org.uncommons.watchmaker.framework.factories.StringFactory;
import org.uncommons.watchmaker.framework.operators.AbstractCrossover;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.RouletteWheelSelection;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

public class SimpleConflictsRunnable implements Runnable {

    static final char[] CONFLICTING_CHARS = "123456789".toCharArray();

    static final char NONCONFLICTING_CHAR = '0';
    
    static final char SEPARATOR_CHAR = '\n';

    static final Random random = new MersenneTwisterRNG();

    
    String INITIAL_ITEM = generateInitialItem();
    
    int ROW_LENGTH = INITIAL_ITEM.indexOf(SEPARATOR_CHAR);
    
    int ROW_COUNT = INITIAL_ITEM.split(Pattern.quote("" + SEPARATOR_CHAR)).length;

    static private String generateInitialItem() {
        int rowLength = 50;
        int rowCount = 20;
        double zeroChance = 0.8;
        
        String[] lines = new String[rowCount];
        for (int i = 0; i < rowCount; i++) {
            lines[i] = generateLine(rowLength, zeroChance);
        }
        return String.join("" + SEPARATOR_CHAR, lines);
    }
    
    static private String generateLine(int length, double zeroChance) {
        String baseString = new StringFactory(CONFLICTING_CHARS, length).generateRandomCandidate(random);
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            boolean addZero = random.nextDouble() < zeroChance;
            resultBuilder.append(addZero ? '0' : baseString.charAt(i));
        }
        return resultBuilder.toString();
    }

    @Override
    public void run() {
        System.out.println("INITIAL:");
        System.out.println(INITIAL_ITEM);
        System.out.println();
        
        runEvolution();
        runSat();
        runMaxSat();
    }
    
    public void runEvolution() {
        final int GENERATION_COUNT = 20000;
        
        CandidateFactory<String> factory = new AbstractCandidateFactory<String>() {
            
            @Override
            public String generateRandomCandidate(Random random) {
                String[] rows = new String[ROW_COUNT];
                String[] initialRows = INITIAL_ITEM.split(Pattern.quote("" + SEPARATOR_CHAR));
                rows[0] = initialRows[0];
                for (int i = 1; i < ROW_COUNT; i++) {
                    rows[i] = shuffleString(initialRows[i], random);
                }
                return String.join("" + SEPARATOR_CHAR, rows);
            }
            
            private String shuffleString(String string, Random random) {
                char[] characters = string.toCharArray();
                
                int randomIndex;
                char temp;
                for (int i = characters.length - 1; i > 0; i--)
                {
                    randomIndex = random.nextInt(i + 1);
                    temp = characters[randomIndex];
                    characters[randomIndex] = characters[i];
                    characters[i] = temp;
                }
                
                return new String(characters);
            }
            
        };
        
        EvolutionaryOperator<String> crossoverOperator = new AbstractCrossover<String>(1, new Probability(0.2)) {
            
            @Override
            protected List<String> mate(String string1, String string2, int crossovers, Random random) {
                String[] lines1 = string1.split(Pattern.quote("" + SEPARATOR_CHAR));
                String[] lines2 = string2.split(Pattern.quote("" + SEPARATOR_CHAR));
                String[] result1Lines = new String[ROW_COUNT];
                String[] result2Lines = new String[ROW_COUNT];
                for (int i = 0; i < ROW_COUNT; i++) {
                    boolean flip = (random.nextDouble() < 0.5);
                    result1Lines[i] = flip ? lines2[i] : lines1[i];
                    result2Lines[i] = flip ? lines1[i] : lines2[i];
                }
                return Arrays.asList(
                    String.join("" + SEPARATOR_CHAR, result1Lines),
                    String.join("" + SEPARATOR_CHAR, result2Lines)
                );
            }
            
        };
        
        EvolutionaryOperator<String> mutationOperator = new EvolutionaryOperator<String>() {

            @Override
            public List<String> apply(List<String> strings, Random random) {
                List<String> result = new ArrayList<String>(strings.size());
                for (String item: strings) {
                    if (random.nextDouble() < 0.5) {
                        boolean doMoreMutation = random.nextDouble() < 0.1;
                        int maxMutations = doMoreMutation ? ROW_COUNT * 3 : ROW_COUNT / 2;
                        for (int i = 0; i < maxMutations; i++) {
                            if (random.nextDouble() < 0.8) {
                                item = mutateItem(item, random);
                            }
                        }
                    }
                    result.add(item);
                }
                return result;
            }
            
            public String mutateItem(String item, Random random) {
                int replaces = (int)(random.nextDouble() * 5);
                for (int i = 0; i < replaces; i++) {
                    item = doReplaceInItem(item, random);
                }
                return item;
            }

            public String doReplaceInItem(String item, Random random) {
                int row = (int)(random.nextDouble() * (ROW_COUNT - 1)) + 1;
                String[] lines = item.split(Pattern.quote("" + SEPARATOR_CHAR));
                char[] lineChars = lines[row].toCharArray();
                List<Integer> nonZeroIndices = new ArrayList<>(lineChars.length);
                for (int i = 0; i < lineChars.length; i++) {
                    char character = lineChars[i];
                    if (character != '0') {
                        nonZeroIndices.add(i);
                    }
                }
                
                if (nonZeroIndices.size() < 2) {
                    return item;
                }
                
                Collections.shuffle(nonZeroIndices, random);
                int index1 = nonZeroIndices.get(0);
                
                int index2 = random.nextInt(ROW_LENGTH - 1);
                if (index2 >= index1) {
                    index2++;
                }
                
                char tempChar = lineChars[index1];
                lineChars[index1] = lineChars[index2];
                lineChars[index2] = tempChar;
                
                lines[row] = new String(lineChars);
                
                return String.join("" + SEPARATOR_CHAR, lines);
            }
            
        };
        
        
        List<EvolutionaryOperator<String>> operators = new ArrayList<EvolutionaryOperator<String>>(2);
        operators.add(crossoverOperator);
        operators.add(mutationOperator);
        EvolutionaryOperator<String> pipeline = new EvolutionPipeline<String>(operators);
        
        final FitnessEvaluator<String> fitnessEvaluator = new FitnessEvaluator<String>() {

            @Override
            public double getFitness(String string, List<? extends String> strings) {
                int conflicts = 0;
                for (int i = 0; i < ROW_LENGTH; i++) {
                    Set<Character> characterSet = new HashSet<>();
                    int characterCount = 0;
                    for (int j = 0; j < ROW_COUNT; j++) {
                        char character = string.charAt(j * (ROW_LENGTH + 1) + i);
                        if (character != NONCONFLICTING_CHAR) {
                            characterCount++;
                            characterSet.add(character);
                        }
                    }
                    conflicts += characterCount - characterSet.size();
                }
                return conflicts;
            }

            @Override
            public boolean isNatural() {
                return false;
            }
            
        };

        SelectionStrategy<Object> selectionStrategy = new RouletteWheelSelection();
        
        EvolutionEngine<String> engine = new GenerationalEvolutionEngine<>(
            factory,
            pipeline,
            fitnessEvaluator,
            selectionStrategy,
            random
        );
        
        engine.addEvolutionObserver(new EvolutionObserver<String>() {
            
            public void populationUpdate(PopulationData<? extends String> data) {
                int n = data.getGenerationNumber() + 1;
                double fitness = data.getBestCandidateFitness();
                if (n == GENERATION_COUNT || fitness == 0d) {
                    System.out.println(
                        "------- " + n + ": " + fitness + "; time: " + data.getElapsedTime() + " -------\n\n" +
                        data.getBestCandidate() +
                        "\n\n"
                    );
                }
            }
            
        });
        
        engine.evolve(40, 10, new TerminationCondition() {
            
            private final TargetFitness targetFitnessCondition = new TargetFitness(0, false);
            
            private final GenerationCount generationCountCondition = new GenerationCount(GENERATION_COUNT);
            
            @Override
            public boolean shouldTerminate(PopulationData<?> populationData) {
                return
                    targetFitnessCondition.shouldTerminate(populationData) ||
                    generationCountCondition.shouldTerminate(populationData)
                ;
            }
            
        });
        
    }
    
    
    
    
    
    
    ///////////// SAT /////////////
    
    
    
    
    
    
    public void runSat() {
        System.out.println("\n\n\n--------- SAT... ---------\n");
        
        char[][] initialMatrix = createInitalMatrix();
        char[][] solutionMatrix = createSolutionMatrix(initialMatrix);
        
        try {
            long startMillis = new Date().getTime();
            
            ISolver solver = SolverFactory.newDefault();
            solver.setTimeout(500);
            solver.newVar(ROW_COUNT * ROW_LENGTH * ROW_LENGTH);
            
            fillSolver(solver, initialMatrix);
            
            if (solver.isSatisfiable()) {
                long endMillis = new Date().getTime();
                long elapsedMillis = endMillis - startMillis;
                
                fillSolutionMatrix(solver, initialMatrix, solutionMatrix);
                String solutionString = solutionMatrixToString(solutionMatrix);
                
                System.out.println("\n\nSOLUTION (" + elapsedMillis + "):\n");
                System.out.println(solutionString);
            } else {
                System.out.println("No solution!");
            }
            
        } catch (ContradictionException e) {
            System.out.println("Contradiction!");
        } catch (TimeoutException e) {
            System.out.println("Timeout!");
        }
    }

    public void runMaxSat() { // TODO
        System.out.println("\n\n\n--------- MAX-SAT... ---------\n");
        
        char[][] initialMatrix = createInitalMatrix();
        char[][] solutionMatrix = createSolutionMatrix(initialMatrix);
        
        try {
            long startMillis = new Date().getTime();
            
            WeightedMaxSatDecorator solver = new WeightedMaxSatDecorator(
                org.sat4j.maxsat.SolverFactory.newDefault()
            );
            solver.setTimeout(500);
            solver.newVar(ROW_COUNT * ROW_LENGTH * ROW_LENGTH);
            
            fillSolver(solver, initialMatrix);

            if (solver.isSatisfiable()) {
                long endMillis = new Date().getTime();
                long elapsedMillis = endMillis - startMillis;
                
                fillSolutionMatrix(solver, initialMatrix, solutionMatrix);
                String solutionString = solutionMatrixToString(solutionMatrix);
                
                System.out.println("\n\nSOLUTION (" + elapsedMillis + "):\n");
                System.out.println(solutionString);
            } else {
                System.out.println("No solution??!");
            }
            
        } catch (ContradictionException e) {
            System.out.println("Contradiction!");
        } catch (TimeoutException e) {
            System.out.println("Timeout!");
        }
    }

    private char[][] createInitalMatrix() {
        char[][] initialMatrix = new char[ROW_COUNT][ROW_LENGTH];
        for (int i = 0; i < ROW_COUNT; i++) {
            for (int j = 0; j < ROW_LENGTH; j++) {
                initialMatrix[i][j] = INITIAL_ITEM.charAt(i * (ROW_LENGTH + 1) + j);
            }
        }
        return initialMatrix;
    }

    private char[][] createSolutionMatrix(char[][] initialMatrix) {
        char[][] solutionMatrix = new char[initialMatrix.length][initialMatrix[0].length];
        for (int i = 0; i < initialMatrix.length; i++) {
            for (int j = 0; j < initialMatrix[0].length; j++) {
                if (i == 0) {
                    solutionMatrix[i][j] = initialMatrix[i][j];
                } else {
                    solutionMatrix[i][j] = '0';
                }
            }
        }
        return solutionMatrix;
    }
    
    private void fillSolver(ISolver solver, char[][] initialMatrix) throws ContradictionException {
        class SolverHelper {
            
            final ISolver solver;
            
            SolverHelper(ISolver solver) {
                this.solver = solver;
            }

            void addHardClause(int... clause) throws ContradictionException {
                VecInt clauseVec = new VecInt(clause);
                if (solver instanceof WeightedMaxSatDecorator) {
                    ((WeightedMaxSatDecorator)solver).addHardClause(clauseVec);
                } else {
                    solver.addClause(clauseVec);
                }
            }

            void addSoftClause(int... clause) throws ContradictionException {
                VecInt clauseVec = new VecInt(clause);
                if (solver instanceof WeightedMaxSatDecorator) {
                    ((WeightedMaxSatDecorator)solver).addSoftClause(clauseVec);
                } else {
                    solver.addClause(clauseVec);
                }
            }
            
        }
        SolverHelper solverHelper = new SolverHelper(solver);
        
        for (int row1 = 1; row1 < ROW_COUNT; row1++) {
            for (int col1 = 0; col1 < ROW_LENGTH; col1++) {
                char character1 = initialMatrix[row1][col1];
                
                if (character1 != NONCONFLICTING_CHAR) {
                    
                    int[] itemVariables = new int[ROW_LENGTH];
                    
                    for (int targetCol = 0; targetCol < ROW_LENGTH; targetCol++) {
                        int variable1 = matrixPosToSatVar(row1, col1, targetCol);

                        {
                            char character2 = initialMatrix[0][targetCol];
                            if (character1 == character2) {
                                solverHelper.addSoftClause(-variable1);
                            }
                        }
                        
                        for (int col2 = 0; col2 < ROW_LENGTH; col2++) {
                            if (col2 != col1) {
                                int variable2 = matrixPosToSatVar(row1, col2, targetCol);

                                solverHelper.addHardClause(-variable1, -variable2);
                            }
                            
                            for (int row2 = 1; row2 < ROW_COUNT; row2++) {
                                char character2 = initialMatrix[row2][col2];
                                int variable2 = matrixPosToSatVar(row2, col2, targetCol);
                                
                                if (row2 != row1 && character1 == character2) {
                                    solverHelper.addSoftClause(-variable1, -variable2);
                                }
                            }
                            
                        }
                        
                        int itemVariable = matrixPosToSatVar(row1, col1, targetCol);
                        itemVariables[targetCol] = itemVariable;
                    }

                    solverHelper.addHardClause(itemVariables);
                    
                }
            }
        }
    }
    
    private void fillSolutionMatrix(ISolver solver, char[][] initialMatrix, char[][] solutionMatrix) {
        for (int literal: solver.model()) {
            if (literal > 0) {
                int variable = Math.abs(literal);
                int[] position = satVarToMatrixPos(variable);
                char character = initialMatrix[position[0]][position[1]];
                solutionMatrix[position[0]][position[2]] = character;
            }
        }
        
        String[] solutionLines = new String[ROW_COUNT];
        for (int i = 0; i < ROW_COUNT; i++) {
            solutionLines[i] = new String(solutionMatrix[i]);
        }
    }
    
    private String solutionMatrixToString(char[][] solutionMatrix) {
        String[] solutionLines = new String[ROW_COUNT];
        for (int i = 0; i < ROW_COUNT; i++) {
            solutionLines[i] = new String(solutionMatrix[i]);
        }
        return String.join("" + SEPARATOR_CHAR, solutionLines);
    }

    public int matrixPosToSatVar(int row, int col, int targetCol) {
        return (row * ROW_LENGTH * ROW_LENGTH) + (col * ROW_LENGTH) + targetCol + 1;
    }
    
    public int[] satVarToMatrixPos(int variable) {
        int[] result = new int[3];
        variable--;
        result[2] = variable % ROW_LENGTH;
        variable /= ROW_LENGTH;
        result[1] = variable % ROW_LENGTH;
        variable /= ROW_LENGTH;
        result[0] = variable;
        return result;
    }
    
}
