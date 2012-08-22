package se.kth.pymdht;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class LookupQueue {

	private static final int REPLICA_SIZE = 5;
	private SortedSet<QueuedNode> queue;
	private SortedSet<QueuedNode> closest_responded_qnodes;
	private Set<Inet4Address> queued_ips;
	private Id info_hash;
	private List<InetSocketAddress> stable_addrs;
	private List<InetSocketAddress> unstable_addrs;
	
	public LookupQueue(Id info_hash, List<InetSocketAddress> unstable_addrs, List<InetSocketAddress> stable_addrs){
		this.info_hash = info_hash;
		this.unstable_addrs = unstable_addrs;
		this.stable_addrs = stable_addrs;
		this.queue = new TreeSet<QueuedNode>();
		this.queued_ips = new HashSet<Inet4Address>();
		this.closest_responded_qnodes = new TreeSet<QueuedNode>();
		
	}
	
	public void on_response(Node src_node, List<Node> nodes){
		QueuedNode qnode = new QueuedNode(src_node,
				src_node.id.distance(this.info_hash));
		assert this.closest_responded_qnodes.size() <= REPLICA_SIZE;
		if (this.closest_responded_qnodes.size() == REPLICA_SIZE){
			QueuedNode last = this.closest_responded_qnodes.last();
			if (qnode.distance.log < last.distance.log){
				this.closest_responded_qnodes.remove(last);
				this.closest_responded_qnodes.add(qnode);
			}
		}
		for (Node node : nodes){
			qnode = new QueuedNode(node, node.id.distance(this.info_hash));
			if (!queued_ips.contains(qnode.node.ip)){
				queued_ips.add(qnode.node.ip);
				queue.add(qnode);
			}
		}
	}
		
	public List<Node> get_nodes_to_query(){
		ArrayList<Node> nodes_to_query = new ArrayList<Node>();

		if (this.queue.size() > 0){
			QueuedNode candidate_qnode = this.queue.first();
			assert this.closest_responded_qnodes.size() <= REPLICA_SIZE;
			System.out.println(closest_responded_qnodes.size() + " closest qnodes");

			for(QueuedNode qnode : closest_responded_qnodes){
				System.out.println("closest log: " + qnode.distance.log);
			}
			if (this.closest_responded_qnodes.size() < REPLICA_SIZE ||
					candidate_qnode.distance.compareTo(this.closest_responded_qnodes.last().distance) < 0){
				this.closest_responded_qnodes.add(candidate_qnode);
				this.queue.remove(candidate_qnode);
				nodes_to_query.add(candidate_qnode.node);
				System.out.println("query to log_distance " + candidate_qnode.distance.log);
				if (this.closest_responded_qnodes.size() > REPLICA_SIZE){
					QueuedNode last = this.closest_responded_qnodes.last();
					this.closest_responded_qnodes.remove(last);					
				}
			}
		}
		else{
			System.out.println("bootstrap");
			Node node;
			if (this.unstable_addrs.size() > 0){
				int num_nodes = Math.min(this.unstable_addrs.size(), GetPeersLookup.NUM_UNSTABLE_PER_ROUND);
				for (int i=0; i<num_nodes; i++){
					node = new Node(this.unstable_addrs.remove(0));
					nodes_to_query.add(node);
				}
			}
			else{
				if (this.stable_addrs.size() > 0){
					node = new Node(this.stable_addrs.remove(0));
					nodes_to_query.add(node);
				}
			}
		}
		return nodes_to_query;
	}	
}