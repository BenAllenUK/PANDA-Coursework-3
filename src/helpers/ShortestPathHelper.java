package helpers;


import models.DataPath;
import models.DataPosition;

import java.util.ArrayList;

public class ShortestPathHelper {

    /**
     *
     * This implementation of shortest path algorithm uses a breadth first search to find shortest paths
     *
     * @param sourceId the id of the source {@link models.DataPosition}
     * @param targetId the id of the target {@link models.DataPosition}
     * @param dataPositions a list of all {@link models.DataPosition}s available to pass through
     * @param dataPaths a list of all {@link models.DataPath}s available to pass along
     * @return A list of {@link models.DataPosition}s through which the shortest path traverses
     */
    public static ArrayList<DataPosition> shortestPath(final int sourceId, final int targetId, final ArrayList<DataPosition> dataPositions, final ArrayList<DataPath> dataPaths){

        ArrayList<SearchHolder> fullList = new ArrayList<SearchHolder>();

        SearchHolder sourceHolder = null;

        for(DataPosition dataPosition : dataPositions){
            SearchHolder searchHolder = new SearchHolder(dataPosition);
            fullList.add(searchHolder);
            if(dataPosition.id == sourceId){
                sourceHolder = searchHolder;
            }
        }

        ArrayList<SearchHolder> queue = new ArrayList<SearchHolder>();

        sourceHolder.discovered = true;
        queue.add(sourceHolder);

        SearchHolder targetHolder = null;

        while(queue.size() > 0){
            SearchHolder searchTerm = queue.iterator().next();

            queue.remove(searchTerm);

            for(DataPath dataPath : dataPaths){
                if(dataPath.id1 == searchTerm.dataPosition.id){
                    for(SearchHolder holder : fullList){
                        if(holder.dataPosition.id == dataPath.id2 && !holder.discovered){
                            holder.previousSearchHolder = searchTerm;
                            holder.discovered = true;
                            if(holder.dataPosition.id == targetId){
                                targetHolder = holder;
                            }else {
                                queue.add(holder);
                            }
                            break;
                        }
                    }
                }else if(dataPath.id2 == searchTerm.dataPosition.id){
                    for(SearchHolder holder : fullList){
                        if(holder.dataPosition.id == dataPath.id1 && !holder.discovered){
                            holder.previousSearchHolder = searchTerm;
                            holder.discovered = true;

                            if(holder.dataPosition.id == targetId){
                                targetHolder = holder;
                            }else {
                                queue.add(holder);
                            }
                            break;
                        }
                    }
                }

                if(targetHolder != null){
                    break;
                }
            }

            if(targetHolder != null){
                break;
            }

        }

        ArrayList<DataPosition> positionsList = null;

        if(targetHolder != null){
            positionsList = new ArrayList<DataPosition>();

            while(targetHolder != null){
                positionsList.add(targetHolder.dataPosition);
                targetHolder = targetHolder.previousSearchHolder;
            }
        }

        if(positionsList == null){
//            System.err.println("positionsList is null for "+sourceId+" -> "+targetId);
        }else if(positionsList.size() == 0){
            System.err.println("positionsList is empty for "+sourceId+" -> "+targetId);
        }



        return positionsList;


    }

    /**
     * This small class allows us to hold DataPositions and keep track of the last
     * {@link models.DataPosition} through which we have travelled
     * á la Dijkstra
     */
    static class SearchHolder {

        public final DataPosition dataPosition;
        public boolean discovered;
        public SearchHolder previousSearchHolder;

        public SearchHolder(DataPosition dataPosition) {
            this.dataPosition = dataPosition;
        }
    }


}
