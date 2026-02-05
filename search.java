import java.util.*;
import java.io.*;

public class search {
    // Grid is 100x100, coordinates are (col, row) with 0..99 valid
    private static final int SIZE = 100;
    private static final int CENTER_COL = 50;
    private static final int CENTER_ROW = 50;
    private static final boolean SORT_OUTPUT = true;

    private static final int TRAVEL_PER_ADJ = 1; //HOURS
    private static final int SEARCH_TIME = 2; //HOURS
    private static final int MAX_AWAKE = 16; //HOURS
    private static final int SLEEP_TIME = 8; //HOURS

    //--------- Data Types ---------
    private static final class Point {
        final int col;
        final int row;
        Point(int col, int row) {
            this.col = col;
            this.row = row;
        }
    }

    /**
     * Sparse visited set using an orthogonal list:
     * - Each row has a sorted linked list by column (right pointers)
     * - Each column has a sorted linked list by row (down pointers)
     *
     * contains() is done by scanning the row list for that row.
     */
    private static final class OrthoVisited {
        private static final class Node {
            final int col, row;
            Node right; // next is row
            Node down;  // next in column
            Node(int col, int row) {
                this.col = col;
                this.row = row;
            }
        }

        private final Node[] rowHeads; // dummy header
        private final Node[] colHeads; // dummy header

        OrthoVisited(int size) {
            rowHeads = new Node [size];
            colHeads = new Node [size];
            for(int r = 0; r < size; r++) {
                rowHeads[r] = new Node(-1, r);
            }
            for(int c = 0; c < size; c++) {
                colHeads[c] = new Node(c, -1);
            }
        }

        boolean contains(int col, int row) {
            if (!inBounds(col, row)) return false;
            Node cur = rowHeads[row].right;
            while (cur != null && cur.col < col) cur = cur.right;
            return cur != null && cur.col == col;
        }

        void add(int col, int row) {
            if (!inBounds(col, row)) return;
            if (contains(col, row)) return;

            Node n = new Node(col, row);

            // Insert into row list (sorted by col)
            Node prevR = rowHeads[row];
            Node curR = prevR.right;
            while (curR != null && curR.col < col) {
                prevR = curR;
                curR = curR.right;
            }
            n.right = curR;
            prevR.right = n;

            // Insert into col list (sorted by row)
            Node prevC = colHeads[col];
            Node curC = prevC.down;
            while (curC != null && curC.row < row) {
                prevC = curC;
                curC = curC.down;
            }
            n.down = curC;
            prevC.down = n;
        }
    }

    // ---------- Simulation state ----------
    private static final class Sim {
        final OrthoVisited visited = new OrthoVisited(SIZE);
        final List<Point> searchedOrder = new ArrayList<>();

        long totalHours = 0;
        int awakeHours = 0;

        int curCol = CENTER_COL;
        int curRow = CENTER_ROW;

        Sim() {
            // Center is considered already searched.
            visited.add(CENTER_COL, CENTER_ROW);
        }

        void sleepNow() {
            totalHours += SLEEP_TIME;
            awakeHours = 0;
            // They return to center to sleep; teleport time is not specified as >0,
            // so we don't add travel time here (sleep itself is 8 hours).
            curCol = CENTER_COL;
            curRow = CENTER_ROW;
        }

        boolean canDo(int travelSteps, int actionHours) {
            return awakeHours + (travelSteps * TRAVEL_PER_ADJ) + actionHours <= MAX_AWAKE;
        }

        void travelTo(int newCol, int newRow) {
            int dist = manhattan(curCol, curRow, newCol, newRow);
            totalHours += (long) dist * TRAVEL_PER_ADJ;
            awakeHours += dist * TRAVEL_PER_ADJ;
            curCol = newCol;
            curRow = newRow;
        }

        void searchSquare(int col, int row) {
            // travel to it first
            travelTo(col, row);

            // search it
            totalHours += SEARCH_TIME;
            awakeHours += SEARCH_TIME;

            visited.add(col, row);
            searchedOrder.add(new Point(col, row));

            // If we've hit the max awake time exactly, sleep immediately.
            if (awakeHours == MAX_AWAKE) {
                sleepNow();
            }
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java search <inputfile.txt>");
            System.exit(1);
        }

        InputData input;
        try {
            input = readInput(args[0]);
        } catch (FileNotFoundException e) {
            System.err.println("Could not open input file: " + args[0]);
            System.exit(1);
            return;
        } catch (IllegalArgumentException e) {
            System.err.println("Input error: " + e.getMessage());
            System.exit(1);
            return;
        }

        Sim sim = new Sim();

        // Process each RS in order
        for (Point rs : input.randomSpots) {

            if (!inBounds(rs.col, rs.row)) 
                continue;

            // Teleport to RS (0 time); we start the RS routine from there.
            sim.curCol = rs.col;
            sim.curRow = rs.row;

            // If RS + N/E/S/W are all already searched, skip to next RS.
            if (neighborhoodFullySearched(sim.visited, rs.col, rs.row)) {
                continue;
            }

            // Targets in order: RS, N, E, S, W
            int[][] targets = new int[][]{
                    {rs.col, rs.row},
                    {rs.col, rs.row - 1}, // north
                    {rs.col + 1, rs.row}, // east
                    {rs.col, rs.row + 1}, // south
                    {rs.col - 1, rs.row}  // west
            };

            for (int[] t : targets) {
                int c = t[0], r = t[1];

                if (!inBounds(c, r)) 
                    continue;
                if (sim.visited.contains(c, r)) 
                    continue;

                int dist = manhattan(sim.curCol, sim.curRow, c, r);

                // If we can't finish traveling + searching another square before sleep, sleep early.
                if (!sim.canDo(dist, SEARCH_TIME)) {
                    sim.sleepNow();
                    break; // sleep interrupts the pattern; next RS after sleep.
                }

                sim.searchSquare(c, r);

                // If searchSquare caused sleeping (awakeHours reset), pattern is interrupted.
                if (sim.awakeHours == 0) {
                    break;
                }
            }
        }

        // Output searched squares in required formatting + final hours.
        List<Point> out = new ArrayList<>(sim.searchedOrder);

        if (SORT_OUTPUT) {
            if (input.outputMode.equals("row")) {
                out.sort(Comparator.comparingInt((Point p) -> p.row)
                                .thenComparingInt(p -> p.col));
            } else { // "col"
                out.sort(Comparator.comparingInt((Point p) -> p.col)
                                .thenComparingInt(p -> p.row));
            }
        }

        for (Point p : out) {
            if (input.outputMode.equals("row")) {
                System.out.println(p.row + " " + p.col);
            } else {
                System.out.println(p.col + " " + p.row);
            }
        }
        System.out.println(sim.totalHours);

    }

    // ---------- Helpers ----------
    private static final class InputData {
        final String outputMode; // "row" or "col"
        final List<Point> randomSpots;
        InputData(String outputMode, List<Point> randomSpots) {
            this.outputMode = outputMode;
            this.randomSpots = randomSpots;
        }
    }

    private static InputData readInput(String filename) throws FileNotFoundException {
        try (Scanner sc = new Scanner(new File(filename))) {

            if (!sc.hasNext()) throw new IllegalArgumentException("Empty file.");

            String mode = sc.next().trim().toLowerCase();
            if (!mode.equals("row") && !mode.equals("col")) {
                throw new IllegalArgumentException("First token must be 'row' or 'col'.");
            }

            List<Integer> nums = new ArrayList<>();
            while (sc.hasNext()) {
                if (!sc.hasNextInt()) {
                    throw new IllegalArgumentException("Non-integer coordinate value found.");
                }
                nums.add(sc.nextInt());
            }

            if (nums.size() % 2 != 0) {
                throw new IllegalArgumentException(
                    "Odd number of integers after mode (coordinates must be pairs)."
                );
            }

            List<Point> rsList = new ArrayList<>();
            for(int i = 0; i < nums.size(); i+= 2) {
                int c = nums.get(i);
                int r = nums. get(i + 1);

                int col, row;
                if(mode.equals("row")){
                    row = c;
                    col = r;
                }
                else {
                    col = c;
                    row = r;
                }

                rsList.add(new Point(col, row));
            }

            return new InputData(mode, rsList);
        }
    }


    private static boolean neighborhoodFullySearched(OrthoVisited visited, int col, int row) {
        // Treat out-of-bounds neighbors as "effectively searched" because they'd be skipped anyway.
        // This makes edge RS behave reasonably with the rule.
        int[][] cells = new int[][]{
                {col, row},
                {col, row - 1},
                {col + 1, row},
                {col, row + 1},
                {col - 1, row}
        };
        for (int[] c : cells) {
            int cc = c[0], rr = c[1];
            if (inBounds(cc, rr) && !visited.contains(cc, rr)) 
                return false;
        }
        return true;
    }

    private static boolean inBounds(int col, int row) {
        return col >= 0 && col < SIZE && row >= 0 && row < SIZE;
    }

    private static int manhattan(int c1, int r1, int c2, int r2) {
        return Math.abs(c1 - c2) + Math.abs(r1 - r2);
    }

}
