package models;

/**
 * This is the model representation of a point on the map
 * x and y are its coordinates
 * id is its identifying number
 */

public class DataPosition {
    public int id;

    public DataPosition(int x, int y, int id) {
        this.id = id;
    }

	public DataPosition(final int id) {
		this.id = id;
	}

	@Override
    public String toString() {
        return "DataPosition{" +
                ", id=" + id +
                '}';
    }

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		final DataPosition that = (DataPosition) o;

		if (id != that.id)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return id;
	}
}
