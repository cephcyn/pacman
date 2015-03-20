
import info.gridworld.actor.Actor;
import info.gridworld.grid.Grid;
import info.gridworld.grid.Location;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author s-zhouj
 */
public class PacMan extends Actor implements PacManInterface {

    private ArrayList<Actor> pellets = new ArrayList<Actor>();
    private ArrayList<Actor> ghosts = new ArrayList<Actor>();
    private Actor closestPellet = null;
    private ArrayList<Location> path = new ArrayList<Location>();
    private int pathstep = 0;

    private Grid<Actor> grid;
    private MyStats myStats;
    private boolean amSuper = false;
    private Color defColor;
    private boolean fullyInitialized = false;

    /**
     * Called after class creation to initialize a Pac-Man. Do not put your
     * Actor in the grid in this method. The world will call
     * start(info.gridworld.grid.Grid<info.gridworld.actor.Actor>,
     * info.gridworld.grid.Location) when you should do this.
     *
     * @param ms
     * @param color
     */
    @Override
    public void initializeStats(MyStats ms, Color color) {
        this.myStats = ms;
        this.setColor(color);
        this.defColor = color;
    }

    /**
     * This method is called when Pac-Man should enter the Maze. Use
     * putSelfInGrid to add yourself to the designated location.
     *
     * @param grid
     * @param lctn
     */
    @Override
    public void start(Grid<Actor> grid, Location lctn) {
        //Set basic stats
        this.grid = grid;
        this.putSelfInGrid(grid, lctn);
        this.setDirection(Location.EAST);
    }

    /**
     * Called when this Pac-Man eats a PowerPellet. Pac-Man should set their
     * color to BLUE when Pac-Man turns super and return to their original color
     * when they've stopped. A Pac-Man may eat ghosts when any other Pac-Man is
     * super.
     *
     * @param bln
     * @param actor
     */
    @Override
    public void superPacMan(boolean bln, Actor actor) {
        if (actor == this) {
            this.amSuper = bln;
            if (bln) {
                myStats.addSuper();
                this.setColor(Color.BLUE);
            } else {
                this.setColor(this.defColor);
            }
        }
    }

    /**
     * Called when Pac-Man is eaten by a Ghost. You should remove yourself from
     * the grid (if you haven't already), and indicate that you've died.
     *
     */
    @Override
    public void eaten() {
        myStats.died();
        this.removeSelfFromGrid();
    }

    /**
     * Tell Ghosts if this Pac-Man is currently super-powered.
     *
     * @return boolean
     */
    @Override
    public boolean isSuperPacMan() {
        return this.amSuper;
    }

    /**
     * Performs one step of action.
     */
    @Override
    public void act() {
        if (!fullyInitialized) {
            //I can't do this earlier since every object is placed on the board in-order
            //Read through the grid to get locations for pellets and ghosts
            ArrayList<Location> locs = grid.getOccupiedLocations();
            for (int i = 0; i < locs.size(); i++) {
                if (grid.get(locs.get(i)) instanceof Pellet) {
                    //if it's a pellet or power pellet (power pellets extend pellet)
                    pellets.add(grid.get(locs.get(i)));
                } else if (grid.get(locs.get(i)) instanceof Ghost) {
                    //if it's a pellet or power pellet (power pellets extend pellet)
                    ghosts.add(grid.get(locs.get(i)));
                }
            }
        }
        Location target = this.getLocation();
        Random rand = new Random();
        //Find the closest pellet
        if (this.pellets.indexOf(closestPellet) < 0) {
            //Find the new closest pellet, if i've already eaten the last one
            this.closestPellet = this.pellets.get(pellets.size() - 1);
            for (Actor pellet : this.pellets) {
                if (Math.sqrt(
                        Math.pow(pellet.getLocation().getRow() - this.getLocation().getRow(), 2)
                        + Math.pow(pellet.getLocation().getCol() - this.getLocation().getCol(), 2)
                ) < Math.sqrt(
                        Math.pow(closestPellet.getLocation().getRow() - this.getLocation().getRow(), 2)
                        + Math.pow(closestPellet.getLocation().getCol() - this.getLocation().getCol(), 2)
                )) {
                    closestPellet = pellet;
                }
            }
        }

        //if there are any pellets left to eat
        if (pellets.size() > 0) {
            //If i'm next to something
            if (adjacentTest(this.getLocation()) != null) {
                target = adjacentTest(this.getLocation());
                pathstep = Integer.MAX_VALUE;
            } else {
                //if i'm not next to something and have walked through my path (or didn't have one)
                if (this.pathstep >= path.size()) {
                    //get a new path and reset my path progress
                    this.pathstep = 0;
                    this.path = optimalStepPath(this.getLocation());
                }
                //follow the steps on my path
                target = this.path.get(pathstep++);
            }
        }

        //If the path want to take goes towards a ghost (and i'm not super), do something that won't lead me towards a ghost. (1st priority)
        while (grid.get(target) instanceof Ghost && !this.isSuperPacMan()) {
            this.setDirection(90 * rand.nextInt(4));
            target = Utility.directionMove(this.getDirection(), this.getLocation(), grid);
        }

        //If I will-eat/ate an edible, count that and remove it from the pelletlist.
        if (grid.get(target) instanceof Pellet) {
            if (grid.get(target) instanceof PowerPellet) {
                this.myStats.addSuper();
                this.superPacMan(true, this);
            } else {
                this.myStats.scorePellet();
            }
            this.pellets.remove(grid.get(target));
        } else if (grid.get(target) instanceof Ghost) {
            if (this.isSuperPacMan()) {
                this.myStats.scoreAteGhost(grid.get(target));
            } else {
                this.myStats.died();
                this.eaten();
            }
        }

        //Set my direction and make my final move.
        this.setDirection(this.getLocation().getDirectionToward(target));
        this.moveTo(target);
    }

    private ArrayList<Location> optimalStepPath(Location current) {
        //Initialize what I need
        ArrayList<SourcedLocationStep> totest = new ArrayList<>();
        ArrayList<Location> solution = null;
        for (int direction : Utility.DIRECTIONS) {
            if (Utility.directionMoveIsValid(direction, current, grid)) {
                totest.add(new SourcedLocationStep(Utility.directionMove(direction, current, grid), null));
            }
        }
        //Run through the list of places to try
        while (totest.size() > 0) {
            //If there is a pellet next to me, return the path that I took in getting there
            if (adjacentTest(totest.get(0)) != null) {
                solution = totest.get(0).sourcePath();
                solution.add(adjacentTest(totest.get(0)));
                break;
            } else {
                //If there isn't, keep testing moves and adding them to the end to test later
                for (int direction : Utility.DIRECTIONS) {
                    //Only add it if it's a valid move and hasn't been used in this chain already
                    if (Utility.directionMoveIsValid(direction, totest.get(0), grid)) {
                        SourcedLocationStep temp = new SourcedLocationStep(Utility.directionMove(direction, totest.get(0), grid), totest.get(0));
                        ArrayList<Location> leadupMap = temp.sourcePath();
                        leadupMap.remove(leadupMap.size() - 1);
                        if (!containsTest(leadupMap, temp) && !current.equals(temp)) {
                            totest.add(temp);
                        }
                    }
                }
            }
            //Remove the current element (it won't be tested again)
            totest.remove(0);
        }
        if (solution != null) {
            return solution;
        }
        ArrayList<Location> gameover = new ArrayList<Location>();
        gameover.add(current);
        return gameover;
    }

    private boolean containsTest(ArrayList<Location> list, Location tofind) {
        for (Location testing : list) {
            if (testing.equals(tofind)) {
                return true;
            }
        }
        return false;
    }

    private Location adjacentTest(Location current) {
        //If I'm adjacent to something I want to eat, go there.
        for (Location surrloc : grid.getValidAdjacentLocations(current)) {
            //if it's not a diagonal (can't go in diagonals :P)
            if (surrloc.getRow() == current.getRow()
                    || surrloc.getCol() == current.getCol()) {
                if (grid.get(surrloc) instanceof Pellet) {
                    return surrloc;
                } else if (this.isSuperPacMan() && grid.get(surrloc) instanceof Ghost) {
                    return surrloc;
                }
            }
        }
        return null;
    }
}
