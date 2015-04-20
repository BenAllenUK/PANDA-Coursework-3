package helpers;

import scotlandyard.Edge;
import scotlandyard.Graph;
import scotlandyard.Node;
import scotlandyard.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by benallen on 13/04/15.
 */
public class GraphHelper {

    public static List<Edge<Integer, Route>> getConnectedEdges(Graph<Integer, Route> graph, Node node){
        List<Edge<Integer, Route>> edges = new ArrayList<Edge<Integer, Route>>();

        for(Edge<Integer, Route> edge: graph.getEdges()){
            if(edge.source().equals(node.data())){
                edges.add(edge);
            }else if(edge.target().equals(node.data())){
                edges.add(edge);
            }
        }
        return edges;
    }

}
