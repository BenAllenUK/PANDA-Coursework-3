package helpers;

import models.DataPath;
import models.DataPosition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ShortestPathHelper {
	private final HashSet<SearchHolder> fullList;
	private final ArrayList<DataPath> dataPaths;

	public ShortestPathHelper (final ArrayList<DataPosition> dataPositions, final ArrayList<DataPath> dataPaths){

		this.dataPaths = dataPaths;
		fullList = new HashSet<SearchHolder>();

		for(DataPosition dataPosition : dataPositions){
			SearchHolder searchHolder = new SearchHolder(dataPosition);
			fullList.add(searchHolder);
		}

	}

    /**
     *
     * This implementation of shortest path algorithm uses a breadth first search to find shortest paths
     *
     * @param sourceId the id of the source {@link models.DataPosition}
     * @param targetId the id of the target {@link models.DataPosition}
     * @return A list of {@link models.DataPosition}s through which the shortest path traverses
     */
    public Set<DataPosition> shortestPath(final int sourceId, final int targetId){

		SearchHolder sourceHolder = null;

		HashSet<SearchHolder> localFullList = new HashSet<SearchHolder>();
		for(SearchHolder searchHolder : fullList){
			final SearchHolder holder = new SearchHolder(searchHolder.dataPosition);
			localFullList.add(holder);
			if(searchHolder.dataPosition.id == sourceId){
				sourceHolder = holder;
			}
		}


        Set<SearchHolder> queue = new HashSet<SearchHolder>();

        sourceHolder.discovered = true;

        SearchHolder targetHolder = null;

		boolean firstRun = true;

//		final ExecutorService threadExecutor = Executors.newFixedThreadPool(2);

        while(firstRun || queue.size() > 0){

			//this helps with speed and efficiency
			SearchHolder searchTerm;
			if(firstRun){
				firstRun = false;
				searchTerm = sourceHolder;
			}else {
				searchTerm = queue.iterator().next();
            	queue.remove(searchTerm);
			}

            for(DataPath dataPath : dataPaths){
                if(dataPath.id1 == searchTerm.dataPosition.id){
                    for(SearchHolder holder : localFullList){
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
                    for(SearchHolder holder : localFullList){
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

        Set<DataPosition> positionsList = null;

        if(targetHolder != null){
            positionsList = new HashSet<DataPosition>();

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
