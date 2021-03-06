package pl.edu.zut.wdsi.puzzle;

import pl.edu.zut.wdsi.hfunctions.HManhattan;
import pl.edu.zut.wdsi.hfunctions.HMisplacedTiles;
import sac.graph.AStar;
import sac.graph.GraphSearchAlgorithm;
import sac.graph.GraphState;
import sac.graph.GraphStateImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Puzzle extends GraphStateImpl {
    private static final int n=3;
    private byte[][] board;
    private int x, y;
    private ZeroPosition zeroPosition = new ZeroPosition();
    private int misplacedPuzzlePiece = 0;
    private int distance = 0;

    public Puzzle() {  //napełniamy konstruktor rozwiązaniem
        board = makeAndFillTheBoard();
        //wspołrzędne zera
        x = 0;
        y = 0;
    }

    public Puzzle (Puzzle parent) {
        board = new byte[n][n];
        for (int i = 0; i < n; i++)
            System.arraycopy(parent.board[i], 0, board[i], 0, n);

        x = parent.x;
        y = parent.y;

        distance = parent.distance;
        zeroPosition = new ZeroPosition(parent.zeroPosition);
        misplacedPuzzlePiece = parent.misplacedPuzzlePiece;
    }

    public Puzzle (int randomCount, int randomSeed) {
        board = makeAndFillTheBoard();

        zeroPosition = new ZeroPosition(0, 0);
        shuffle(randomCount, randomSeed);
    }

    @Override
    public int hashCode() {
        byte[] linear = new byte[n*n];
        int value = 0;
        for (int i = 0; i < n; i++)
            for(int j = 0; j < n; j++) linear[value++] = board[i][j];

        return Arrays.hashCode(linear);
    }

    @Override
    public List<GraphState> generateChildren() {
        List<GraphState> list = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            Puzzle child = new Puzzle(this);
            Moves move = Moves.values()[i];
            if (child.move(move)) {
                child.setMoveName(move.toString());
                list.add(child);
            }
        }

        return list;
    }

    @Override
    public boolean isSolution() {
        return misplacedPuzzlePiece == 0 && distance == 0;
    }

    private byte[][] makeAndFillTheBoard() {
        board = new byte[n][n];
        byte value = 0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) board[i][j] = value++;

        return board;
    }

    private void swap (int x1, int y1, int x2, int y2) {
        byte tmp = board[y1][x1];

        board[y1][x1] = board[y2][x2];
        board[y2][x2] = tmp;
    }

    private void swap (ZeroPosition position1, ZeroPosition position2) {
        swap(position1.getX(), position1.getY(), position2.getX(), position2.getY());
    }

    private int findPos (byte puzzlePiece, byte[] board) {
        for (int i = 0; i < n*n; ++i)
            if (board[i] == puzzlePiece)
                return i;

        return -1;
    }

    private boolean move (Moves move) {
        ZeroPosition elderPosition = new ZeroPosition(zeroPosition);

        switch(move) {
            case GOUP:
                if (zeroPosition.getY() > 0) zeroPosition.setY(zeroPosition.getY()-1);
                else return false;

                swap(zeroPosition, elderPosition);
                break;
            case GODOWN:
                if (zeroPosition.getY() < n - 1) zeroPosition.setY(zeroPosition.getY()+1);
                else return false;

                swap(zeroPosition, elderPosition);
                break;
            case GOLEFT:
                if (zeroPosition.getX() > 0) zeroPosition.setX(zeroPosition.getX()-1);
                else return false;

                swap(zeroPosition, elderPosition);
                break;
            case GORIGHT:
                if (zeroPosition.getX() < n - 1) zeroPosition.setX(zeroPosition.getX()+1);
                else return false;

                swap(zeroPosition, elderPosition);
                break;
        }

        refreshPuzzlePieces();
        return true;
    }

    private void shuffle (int rCount, int rSeed) {
        Random random = new Random(rSeed);

        for(int i = 0; i < rCount; ++i)
            if (!move(Moves.values()[random.nextInt(4)])) i--;
    }

    private int calculateDistance(byte tile, byte[] board) {
        int position = findPos(tile, board);
        if (tile != position)
            if (position > tile) return position - tile;
            else return tile - position;
        else return 0;
    }

    private void refreshDistance() {
        distance = 0;
        int k = 0;

        byte[] boardCopy = new byte[n * n];

        for (int i = 0; i < n; ++i)
            for (int j = 0; j < n; ++j) {
                boardCopy[k] = board[i][j];
                k++;
            }

        for (int i = 1; i < n*n; ++i) distance += calculateDistance((byte)i, boardCopy);

    }

    private void refreshMisplacedPuzzlePieces() {
        misplacedPuzzlePiece = 0;
        byte value = 0;

        for (int i = 0; i < n; ++i) {
            int j = 0;
            while (j < n) {
                if (board[i][j] != value) misplacedPuzzlePiece++;
                value++;
                ++j;
            }
        }
    }

    private void refreshPuzzlePieces() {
        refreshMisplacedPuzzlePieces();
        refreshDistance();
    }

    public int getMisplacedPuzzlePiece() {
        return misplacedPuzzlePiece;
    }

    public int getDistance() {
        return distance;
    }

    private static void provideTests (Puzzle puzzle, int tests, int[] testSeeds, String hFunction) {
        Random random = new Random();
        GraphSearchAlgorithm alg = new AStar(puzzle);

        int time = 0;
        int close = 0;
        int open = 0;
        int pathLength = 0;

        for (int i = 0; i < tests; ++i) {
            testSeeds[i] = random.nextInt();
            puzzle.shuffle(1000, testSeeds[i]);

            alg.execute();

            for (GraphState solution : alg.getSolutions())
                pathLength += solution.getMovesAlongPath().size();

            time += alg.getDurationTime();
            close += alg.getClosedStatesCount();
            open += alg.getOpenSet().size();
        }
        printResults(time, tests, close, open, pathLength, hFunction);
    }

    private static void printResults (int time, int tests, int close, int open, int pathLength, String hFunction) {

        System.out.println("---------------------------"+hFunction+"---------------------------");
        System.out.print("Medium time: ");
        System.out.println((double)time/tests);
        System.out.print("Medium close set: ");
        System.out.println((double)close/tests);
        System.out.print("Medium open set: ");
        System.out.println((double)open/tests);
        System.out.print("Medium path length: ");
        System.out.println((double)pathLength/tests);
    }

    public static void main(String... args) {

        int tests = 100;
        int[] testSeeds = new int[tests];

        Puzzle puzzle = new Puzzle();
        setHFunction(new HManhattan());
        provideTests(puzzle, tests, testSeeds, "Manhattan");

        setHFunction(new HMisplacedTiles());
        provideTests(puzzle, tests, testSeeds, "MisplacedTiles");


    }
}
