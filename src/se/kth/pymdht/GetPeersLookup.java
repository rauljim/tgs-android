package se.kth.pymdht;

import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class GetPeersLookup{
	
	static final int MAX_UNSTABLE_ROUNDS = 5;
	static final int NUM_UNSTABLE_PER_ROUND = 8;
	private Id info_hash;
	private LookupQueue lookup_queue;
	private RandomId my_id;
	private List<ByteBuffer> all_cpeers;
	private List<ByteBuffer> recent_cpeers;

	public GetPeersLookup(Id info_hash, OverlayBootstrapper bootstrapper){
		this.my_id = new RandomId();
		this.info_hash = info_hash;
		this.lookup_queue = new LookupQueue(info_hash,
				bootstrapper.get_sample_unstable_addrs(NUM_UNSTABLE_PER_ROUND * MAX_UNSTABLE_ROUNDS),
				bootstrapper.get_shuffled_stable_addrs());
		this.all_cpeers = new ArrayList<ByteBuffer>();
		this.recent_cpeers = new ArrayList<ByteBuffer>();
}
	
	public void on_response(IncomingMsg msg){
		for (ByteBuffer cpeer : msg.cpeers){
			if (!this.all_cpeers.contains(cpeer)){
				this.all_cpeers.add(cpeer);
				this.recent_cpeers.add(cpeer);
			}
		}
		this.lookup_queue.on_response(msg.src_node, msg.all_nodes);
	}
	
	public List<ByteBuffer> get_cpeers(){
		List<ByteBuffer> cpeers = recent_cpeers;
		this.recent_cpeers = new ArrayList<ByteBuffer>();
		return cpeers;
	}
	
	
	public List<DatagramPacket> get_datagrams(){
		List<Node> nodes = lookup_queue.get_nodes_to_query();
		List<DatagramPacket> datagrams = new ArrayList<DatagramPacket>(nodes.size());
		byte[] data = new OutgoingGetPeersQuery(this.my_id, this.info_hash).get_bencoded();
		for (Node node : nodes){
			try {
				datagrams.add(new DatagramPacket(data, data.length, node.addr));
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		return datagrams;
	}
}
