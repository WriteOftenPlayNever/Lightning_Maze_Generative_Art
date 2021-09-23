package com.processing.sketch;

import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Sketch extends PApplet {

    // A global Random variable to power all the random generation in the sketch
    static Random r = new Random();

    public void settings() {
        // Set the canvas size to be A2 ratio and of a decent size
        // and save this lovely generative art
        size(1080, (int) Math.floor(1080 * Math.sqrt(2)), PDF, "COMPLETE - " + r.nextInt() + ".pdf");
    }

    public void setup() {
        // Centre rectmode makes all the square placements easier for the maze
        rectMode(CENTER);

        // White background
        // Respecting that this would have been printed to real paper
        background(255);

        // Instantiate, generate, and solve the maze respectively
        Maze theMaze = new Maze(32, 45);
        theMaze.generate(theMaze.startCell);
        theMaze.drawToCanvas();

    }

    /**
     * The Maze data structure
     * A 64x64 2D array of cells
     * With functions for generating a random maze
     * and solving a generated maze
     * as well as miscellaneous convenience functions
     */
    public class Maze {

        /**
         * Width and height of the maze (in Cells)
         * The Cell array
         * and the start and end cell of the maze respectively
         * The data types stored in the Maze class
         */
        private int mWidth;
        private int mHeight;
        private Cell[][] mazeCells;
        private Pair<Integer, Integer> startCell;
        private Pair<Integer, Integer> endCell;

        /**
         * Maze constructor.
         * Instantiates a new Maze.
         *
         * @param mWidth  the maze width
         * @param mHeight the maze height
         */
        Maze(int mWidth, int mHeight) {
            // Store the dimensions of the array for later
            // Often used in other functions
            this.mWidth = mWidth;
            this.mHeight = mHeight;

            // Instantiate the maze cell array to the correct dimensions
            this.mazeCells = new Cell[mWidth][mHeight];

            // Randomly select one cell at the top and bottom of the maze array
            // to serve as the start and end of the maze
            this.startCell = new Pair<Integer, Integer>(r.nextInt(mWidth), 0);
            this.endCell = new Pair<Integer, Integer>(r.nextInt(mWidth), mHeight - 1);

            // Generate the maze into the array, and solve it to create the lightning path
            this.generate(startCell);
            this.solve(getViaCoords(startCell));
        }

        /**
         * Generates the maze from an array of nulls
         * Uses the random selection depth first search method
         * Explanation and pseudocode can be found
         * here: https://en.wikipedia.org/wiki/Maze_generation_algorithm
         *
         * @param coords the coordinates of the current cell, for tracking recursion
         */
        public void generate(Pair<Integer, Integer> coords) {
            // Instantiate the current Cell, so we know we've visited it
            mazeCells[coords.first][coords.second] = quickCell(coords);

            // Get a List of all the directions, to be pared down
            ArrayList<Dir> validDirections = new ArrayList<Dir>(Arrays.asList(Dir.values()));

            // While there are untried directions, do the following;
            while (validDirections.size() > 0) {
                // Select a random direction from the list
                Dir direction = validDirections.remove(r.nextInt(validDirections.size()));

                // If it's invalid, discard it
                if (validateDirection(direction, coords)) {
                    // Grab the Cell in that direction
                    Cell target = getViaCoords(directionToCoords(direction, coords));

                    // If it is untraversed add a connection to it and recursively visit it
                    if (target == null) {
                        getViaCoords(coords).addConnection(direction);
                        generate(directionToCoords(direction, coords));
                    }
                }
            }
        }

        /**
         * Solve the maze recursively using depth-first search
         *
         * @param cell  the current cell
         * @return boolean value of whether that cell is on the lightning path
         */
        public boolean solve(Cell cell) {
            // This cell has been visited, update it to reflect that
            cell.visited = true;

            // For each of the cells connections, visit that cell and call this function recursively
            for (Dir direction : cell.connections) {
                Cell target = getViaCoords(directionToCoords(direction, cell.coords));
                // Only visit a cell that hasn't been visited before
                // To avoid infinite loops and stack problems
                if (!target.visited) {
                    // If we're finished, turn the cell yellow
                    // and propagate back up the stack doing the same
                    if (target.coords.equals(endCell)) {
                        // Adding some random variation to the yellow of the lightning
                        int yellowness = 200 + r.nextInt(60);
                        target.rgb = new int[]{yellowness, yellowness - r.nextInt(20), 0};
                        return true;
                    } else {
                        // If we haven't finished, call recursively
                        boolean onTrack = solve(target);

                        if (onTrack) {
                            // If we finished somewhere down this line, turn the cell
                            // yellow and continue backpropagation
                            // Unless it's a cloud, leave that gray
                            if (cell.rgb[2] == 255) {
                                int yellowness = 200 + r.nextInt(60);
                                target.rgb = new int[] {yellowness, yellowness - r.nextInt(20), 0};
                            }
                            return true;
                        }
                    }
                }
            }

            // If none of the directions lead to the end return false
            return false;
        }


        /**
         * Draws the maze to canvas.
         */
        public void drawToCanvas() {
            // Some variables for later, mostly derived from the size of the window
            int mazeWidth = (int) Math.floor(0.8 * width);
            int mazeOffsetX = (int) Math.floor(0.1 * width);
            int mazeOffsetY = (int) Math.floor(0.1 * height);
            int squaresize = (mazeWidth / (this.mWidth + 3)) - 2;


            // First Cell iteration
            // Does not handle connections, just the colour of the cells themselves
            for (int x = 0; x < mWidth; x++) {
                for (int y = 0; y < mHeight; y++) {
                    // Get the cell in question
                    Cell cell = getViaCoords(new Pair<Integer, Integer>(x, y));

                    // Fill it with its designated colour
                    fill(cell.rgb[0], cell.rgb[1], cell.rgb[2]);

                    // Set up the stroke correctly, to simulate "walls" to the maze
                    // and also to simulate "rain" in this storm, and cloud in the... clouds
                    strokeWeight(4);
                    int cloudLayer = (mHeight / 15);
                    if (y < (cloudLayer + Math.abs(Math.sin(x) + Math.cos(x)) * cloudLayer) + x/5) {
                        // Randomly generate cloud colour
                        int grayness = 100 + r.nextInt(90);
                        stroke(grayness, grayness + 15, grayness + 15);
                    } else {
                        // Otherwise, a gentle rain blue, adjusted for altitude
                        int alt = (mHeight - y - (x/2) - 10) * 4;
                        stroke(51 - alt, 102 - alt, 255 - alt);
                    }


                    // Use the offset and variables to draw the Cell in position
                    square(mazeOffsetX + (x * (squaresize + 6)),
                            mazeOffsetY + (y * (squaresize + 6)),
                            squaresize + 5);
                }
            }

            // Second Cell iteration
            // This iteration handles connections, and is separate because
            // these joining squares need to sit above the stroke "walls" of the previous layer
            for (int x = 0; x < mWidth; x++) {
                for (int y = 0; y < mHeight; y++) {
                    // Get the cell in question
                    Cell cell = getViaCoords(new Pair<Integer, Integer>(x, y));

                    // turn off stroke, it would interfere with the effect
                    noStroke();

                    // set fill to be the fill colour of the Cell for each connection of that Cell
                    fill(cell.rgb[0], cell.rgb[1], cell.rgb[2]);

                    // For each connection, draw the connection offset from the Cell location
                    for (Dir direction : cell.connections) {
                        switch (direction) {
                            case UP:
                                // Negative y offset
                                square(mazeOffsetX + (x * (squaresize + 6)),
                                        mazeOffsetY + (y * (squaresize + 6)) - 8,
                                        squaresize - 4);
                                break;
                            case DOWN:
                                // Positive y offset
                                square(mazeOffsetX + (x * (squaresize + 6)),
                                        mazeOffsetY + (y * (squaresize + 6)) + 8,
                                        squaresize - 4);
                                break;
                            case LEFT:
                                // Negative x offset
                                square(mazeOffsetX + (x * (squaresize + 6)) - 8,
                                        mazeOffsetY + (y * (squaresize + 6)),
                                        squaresize - 4);
                                break;
                            case RIGHT:
                                // Positive x offset
                                square(mazeOffsetX + (x * (squaresize + 6)) + 8,
                                        mazeOffsetY + (y * (squaresize + 6)),
                                        squaresize - 4);
                                break;
                        }
                    }
                }
            }
        }

        /**
         * A convenience function to grab Cells at specific coordinates
         * while using the generic Pair class instead.
         * This is a common Pair use case, now generified into a function
         *
         * @param coords Coordinates of the required Cell
         * @return The Cell at the given coords
         */
        private Cell getViaCoords(Pair<Integer, Integer> coords) {
            return mazeCells[coords.first][coords.second];
        }

        /**
         * Validate whether you can go a particular direction from a given location
         * Very useful in avoid null pointer exceptions during the various
         * depth first searches of the maze
         *
         * @param direction The direction to be tested
         * @param coords The current location
         * @return Whether that direction can be traversed without leaving the bounds of the Maze array
         */
        private boolean validateDirection(Dir direction, Pair<Integer, Integer> coords) {

            // Make sure that the direction doesn't point off the edge of the array
            switch (direction) {
                case UP:
                    // Up
                    if (coords.second == 0) {
                        return false;
                    }
                    break;
                case DOWN:
                    // Down
                    if (coords.second >= (mHeight - 1)) {
                        return false;
                    }
                    break;
                case LEFT:
                    // Left
                    if (coords.first == 0) {
                        return false;
                    }
                    break;
                case RIGHT:
                    // Right
                    if (coords.first >= (mWidth - 1)) {
                        return false;
                    }
                    break;
            }

            // If none of the above cases fire, then the direction is safe and we return true
            return true;
        }

        /**
         * A convenience function to get the coordinates of a neighbouring Cell, in a given direction
         * Contains no validation, that is assumed to be handled by validateDirection()
         *
         * @param direction The direction to take the cellf rom
         * @param coords The current location
         * @return The new coordinates after having moved in the given direction
         */
        private Pair<Integer, Integer> directionToCoords(Dir direction, Pair<Integer, Integer> coords) {
            // Offset the coords based on direction with swtich-case
            switch (direction) {
                case UP:
                    return new Pair<Integer, Integer>(coords.first, coords.second - 1);
                case DOWN:
                    return new Pair<Integer, Integer>(coords.first, coords.second + 1);
                case LEFT:
                    return new Pair<Integer, Integer>(coords.first - 1, coords.second);
                case RIGHT:
                    return new Pair<Integer, Integer>(coords.first + 1, coords.second);
            }

            // This return statement doesn't DO anything, but the IDE requires it for compilation
            return directionToCoords(direction, coords);
        }

        /**
         * Just a small convenience function to streamline creating cells.
         * Simplified since the Maze instance knows its own width and height
         * and can pass that into the constructor automatically via this function
         *
         * @param coords Coordinates of the Cell to be created
         * @return The newly created Cell instance
         */
        private Cell quickCell(Pair<Integer, Integer> coords) {
            return new Cell(false, mWidth, mHeight, coords);
        }
    }

    /**
     * The type Cell.
     */
    public class Cell {

        /**
         * The data held within a cell;
         * Whether it has been visited by the solver (needed for DFS)
         * The width and height of the maze, used for calculations
         * The rgb values for the colour of the cell
         * The coordinates of the cell
         * and a list of the outgoing connections of the cell
         */
        private boolean visited;
        private final int mWidth;
        private final int mHeight;
        private int[] rgb;
        private final Pair<Integer, Integer> coords;
        private ArrayList<Dir> connections = new ArrayList<Dir>();

        /**
         * Instantiates a new Cell.
         *
         * @param visited the visited
         * @param mWidth  the m width
         * @param mHeight the m height
         * @param coords  the coords
         */
        public Cell(boolean visited, int mWidth, int mHeight, Pair<Integer, Integer> coords) {
            // Standard constructor instantiation
            this.visited = visited;
            this.mWidth = mWidth;
            this.mHeight = mHeight;
            this.coords = coords;

            // Turn the cell blue-ish for sky effect
            // Or gray-ish above a certain y-value for clouds, modulated with trig functions
            // This can be later overridden if this is a lightning cell
            int cloudLayer = (mHeight / 15);
            if (coords.second < (cloudLayer + Math.abs(Math.sin(coords.first) + Math.cos(coords.first)) * cloudLayer) + coords.first/5) {
                // Randomly generate gray colour
                int grayness = 160 + r.nextInt(90);
                this.rgb = new int[] {grayness, grayness, grayness};
            } else {
                // Randomly generate blue and blue derivative colours
                this.rgb = new int[] {150 - r.nextInt(55), 255 - r.nextInt(25), 255};
            }
        }

        /**
         * Add a connection to another CEll.
         *
         * @param direction Rhe direction of the connection
         */
        public void addConnection(Dir direction) {
            // Switch for which direction we're going in
            switch (direction) {
                case UP:
                    // Can't connect up at the top of the maze
                    if (coords.second != 0) {
                        // Connection is valid, add to connections list
                        connections.add(direction);
                    }
                    break;
                case DOWN:
                    // Can't connect down at the bottom of the maze
                    if (coords.second != (mHeight - 1)) {
                        // Connection is valid, add to connections list
                        connections.add(direction);
                    }
                    break;
                case LEFT:
                    // Can't connect left if at the side of the maze
                    if (coords.first != 0) {
                        // Connection is valid, add to connections list
                        connections.add(direction);
                    }
                case RIGHT:
                    // Can't connect right at the other side of the maze
                    if (coords.first != (mWidth - 1)) {
                        // Connection is valid, add to connections list
                        connections.add(direction);
                    }
                    break;
            }
        }
    }


    /**
     * The direction enum, used universally to differentiate
     * all directions in the sketch.
     * Self-explanatory.
     */
    enum Dir {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }


    /**
     * Generic pair class that I've used a few times before
     * very useful for handling coordinates
     *
     * @param <A> the first generic parameter
     * @param <B> the second generic parameter
     */
    public class Pair<A, B> {

        /**
         * The Pair's generic data
         */
        public A first;
        public B second;

        /**
         * Instantiates a new Pair.
         *
         * @param first  the first
         * @param second the second
         */
        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        /**
         * need to be able to compare Pairs, though this does rely
         * on the generic data having also overridden the .equals() function
         * so it isn't a deep comparison, but is serviceable here
         *
         * @param pair the pair to compare
         * @return whether the pairs are equal (shallow)
         */
        public boolean equals(Pair<A, B> pair) {
            return this.first == pair.first && this.second == pair.second;
        }
    }
}
