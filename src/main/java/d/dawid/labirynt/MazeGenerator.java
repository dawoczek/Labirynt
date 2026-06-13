package d.dawid.labirynt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MazeGenerator {

    private final int cols;
    private final int rows;
    private final boolean[][] visited;
    private final boolean[][] wallRight;
    private final boolean[][] wallDown;
    private final Random random;

    private int startCol = 0;
    private int startRow = 0;
    private int endCol;
    private int endRow;

    public MazeGenerator(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.endCol = cols - 1;
        this.endRow = rows - 1;
        this.visited = new boolean[cols][rows];
        this.wallRight = new boolean[cols][rows];
        this.wallDown = new boolean[cols][rows];
        this.random = new Random();

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                wallRight[x][y] = true;
                wallDown[x][y] = true;
            }
        }

        generate(0, 0);
    }

    private void generate(int cx, int cy) {
        visited[cx][cy] = true;

        List<Integer> directions = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(directions, random);

        for (int dir : directions) {
            int nx = cx, ny = cy;

            switch (dir) {
                case 0 -> ny--; // góra
                case 1 -> nx++; // prawo
                case 2 -> ny++; // dół
                case 3 -> nx--; // lewo
            }

            if (nx >= 0 && nx < cols && ny >= 0 && ny < rows && !visited[nx][ny]) {
                if (dir == 1) wallRight[cx][cy] = false;
                if (dir == 3) wallRight[nx][ny] = false;
                if (dir == 2) wallDown[cx][cy] = false;
                if (dir == 0) wallDown[nx][ny] = false;

                generate(nx, ny);
            }
        }
    }

    public int[][] getGrid() {
        int gridW = cols * 2 + 1;
        int gridH = rows * 2 + 1;
        int[][] grid = new int[gridW][gridH];

        // Wypełnij wszystko ścianami
        for (int x = 0; x < gridW; x++) {
            for (int y = 0; y < gridH; y++) {
                grid[x][y] = 1;
            }
        }

        // Otwórz komórki i przejścia
        for (int cx = 0; cx < cols; cx++) {
            for (int cy = 0; cy < rows; cy++) {
                int gx = cx * 2 + 1;
                int gy = cy * 2 + 1;

                grid[gx][gy] = 0;

                if (!wallRight[cx][cy] && cx + 1 < cols) {
                    grid[gx + 1][gy] = 0;
                }

                if (!wallDown[cx][cy] && cy + 1 < rows) {
                    grid[gx][gy + 1] = 0;
                }
            }
        }

        // Przebijamy wejście i wyjście w grubym zewnętrznym murze
        grid[1][0] = 0; // wejście
        grid[gridW - 2][gridH - 1] = 0; // wyjście

        return grid;
    }

    public int getStartGridX() { return startCol * 2 + 1; }
    public int getStartGridZ() { return startRow * 2 + 1; }
    public int getEndGridX() { return endCol * 2 + 1; }
    public int getEndGridZ() { return endRow * 2 + 1; }
}