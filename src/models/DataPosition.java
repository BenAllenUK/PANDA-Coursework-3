package models;

/**
 * This is the model representation of a point on the map
 * x and y are its coordinates
 * id is its identifying number
 */

public class DataPosition {
    public int x;
    public int y;
    public int id;

    public DataPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public DataPosition(int x, int y, int id) {
        this.x = x;
        this.y = y;
        this.id = id;
    }

    @Override
    public String toString() {
        return "DataPosition{" +
                "x=" + x +
                ", y=" + y +
                ", id=" + id +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataPosition that = (DataPosition) o;

        if (id != that.id) return false;
        if (x != that.x) return false;
        if (y != that.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + id;
        return result;
    }
}
