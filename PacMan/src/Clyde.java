
import info.gridworld.grid.Location;

/**
 *
 * @author s-zhouj
 */
public class Clyde extends Blinky {

    /**
     * Returns the target location that the ghost wants to get to when it's in
     * regular mode.
     *
     * @return
     */
    public Location regularTarget() {
        if (Utility.manhattanDistance(this.getLocation(), getPacMan().getLocation()) < 8) {
            return super.regularTarget();
        }
        return scatterTarget();
    }

    /**
     * Returns the target location that the ghosts wants to get to when it's in
     * scatter mode.
     *
     * @return
     */
    public Location scatterTarget() {
        return new Location(getGrid().getNumRows(), getGrid().getNumRows());
    }
}
