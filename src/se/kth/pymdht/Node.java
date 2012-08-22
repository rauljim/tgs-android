package se.kth.pymdht;

import java.net.Inet4Address;
import java.net.InetSocketAddress;

public class Node {
	public InetSocketAddress addr;
	public Id id;
	public Inet4Address ip;
	
	public Node(InetSocketAddress addr, Id node_id){
		this.addr = addr;
		this.id = node_id;
		this.ip = (Inet4Address) addr.getAddress();
	}
	
	public Node(InetSocketAddress addr){
		this.addr = addr;
		this.id = null;
		this.ip = (Inet4Address) addr.getAddress();
	}

	public Id distance(Node other){
		return this.id.distance(other.id);
	}

}
