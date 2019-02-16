package org.qcri.rheem.core.optimizer.mloptimizer.api;

import java.util.LinkedHashMap;

/**
 * Created by migiwara on 08/07/17.
 */
public class TopologyInstance {
    public LinkedHashMap<Integer, String> getNodes() {
        return nodes;
    }

    public void setNodes(LinkedHashMap<Integer, String> nodes) {
        this.nodes = nodes;
    }

    private LinkedHashMap<Integer,String> nodes;

    public void add(int nodeId, String nodeName){
        this.nodes.put(nodeId,nodeName);
    }
}
