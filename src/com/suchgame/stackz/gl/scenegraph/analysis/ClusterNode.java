package com.suchgame.stackz.gl.scenegraph.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A cluster node combines various nodes into one to reduce the complexity
 * of the linearization computation.
 * 
 * @author Matthias Schicker
 */
public class ClusterNode extends AnalysisNode {
	public ArrayList<AnalysisNode> nodes = new ArrayList<AnalysisNode>();

	private static Comparator<AnalysisNode> clusterZlevelComparator = new Comparator<AnalysisNode>() {
		@Override
		public int compare(AnalysisNode a, AnalysisNode b) {
			int aZ = a.node.clusterZLevel;
			int bZ = b.node.clusterZLevel;
			return aZ < bZ ? -1 : (aZ > bZ ? 1 : 0);
		}
	};
	
	public ClusterNode(Integer key, AnalysisNode first) {
		super(key, first.node);
		pathToRoot = first.pathToRoot;
		
		nodes.add(first);
	}
	
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer("<");
		ret.append(nodes.get(0).toString());
		for (int i = 1; i < nodes.size(); i++) ret.append(", ").append(nodes.get(i).toString());
		ret.append('>');
		return ret.toString();
	}

	/**
	 * Sorts all contained nodes according to their clusterZLevel. Should be called
	 * before unclustering a linearization.
	 */
	public void sort(){
		Collections.sort(nodes, clusterZlevelComparator);
	}
	
	public void add(AnalysisNode toAdd) {
		nodes.add(toAdd);
	}

	public int size() {
		return nodes.size();
	}
	
	public boolean contains(AnalysisNode a) {
		return nodes.contains(a);
	}
	
	@Override
	public boolean isCluster() {
		return true;
	}
}
