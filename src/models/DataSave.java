package models;

import java.util.ArrayList;

/**
 * Created by rory on 20/04/15.
 */
public class DataSave {
	public ArrayList<DataPosition> positionList;
	public ArrayList<DataPath> pathList;

	public DataSave(ArrayList<DataPosition> mPositionList, ArrayList<DataPath> mPathList) {
		positionList = mPositionList;
		pathList = mPathList;
	}
}