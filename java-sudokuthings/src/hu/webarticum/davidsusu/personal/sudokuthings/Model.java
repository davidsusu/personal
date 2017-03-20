package hu.webarticum.davidsusu.personal.sudokuthings;

public class Model {
    
    private final int blockWidth;

    private final int blockHeight;

    private final int size;

    private final int[][] data;
    
    public Model(int blockWidth, int blockHeight) {
        this.blockWidth = blockWidth;
        this.blockHeight = blockHeight;
        this.size = blockWidth * blockHeight;
        this.data = new int[size][size];
    }

    public int getBlockWidth() {
        return blockWidth;
    }

    public int getBlockHeight() {
        return blockHeight;
    }
    
    public int getSize() {
        return size;
    }
    
    public void set(int row, int col, int value) {
        data[row][col] = value;
    }
    
    public int get(int row, int col) {
        return data[row][col];
    }
    
}
