package models;

import java.awt.geom.Path2D;

// Note: Removed onSelectedMethod from original

public class DataPath {
    public int id1;
    public int id2;
    public int[] pathXCoords;
    public int[] pathYCoords;

    private transient int movingCoordIndex = -1;

    public DataPath(int id1, int id2) {
        this.id1 = id1;
        this.id2 = id2;
    }

    public Path2D getPath(){
        Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD, pathXCoords.length);
        for (int i = 0; i < pathXCoords.length; i++) {
            if (i == 0) {
                path.moveTo(pathXCoords[i], pathYCoords[i]);
            } else {
                path.lineTo(pathXCoords[i], pathYCoords[i]);
            }
        }
        return path;
    }
    /**
     *
     * This updates the x and y coordinates of the selected coordinate
     *
     * @param x the x position of the click
     * @param y the y position of the click
     */
    public void onPointDrag(int x, int y) {
        pathXCoords[movingCoordIndex] = x;
        pathYCoords[movingCoordIndex] = y;
    }

    /**
     * Clears the {@link models.DataPath#movingCoordIndex}
     */
    public void onDragStop() {
        movingCoordIndex = -1;
    }
}
