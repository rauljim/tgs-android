package se.kth.pymdht;

import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import se.kth.pymdht.Id.IdError;


public class IncomingMsg {
	
	static  class MsgError extends Exception {
		private static final long serialVersionUID = 1L;
	}

	private DatagramPacket _datagram;
	private Map<ByteBuffer, Object> top_dict;
	private Vector<Node> nodes;
	public Vector<Node> all_nodes;
	public List<ByteBuffer> cpeers;
	private Map<ByteBuffer, Object> r_dict;
	public Node src_node;
	
	public IncomingMsg(DatagramPacket datagram) throws MsgError{
		_datagram = datagram;
		
		try{//  bencode.decode may raise bencode.DecodeError
			Bencode b = new Bencode(new ByteArrayInputStream(_datagram.getData()));
			this.top_dict = (Map<ByteBuffer, Object>) b.getRootElement();
			//  type == response 
			if (!(MsgConst.RESPONSE.equals(
					this.top_dict.get(MsgConst.TYPE)))){
				throw new MsgError();
			}
			this.r_dict = (Map<ByteBuffer, Object>) this.top_dict.get(MsgConst.RESPONSE);
			
			ByteBuffer bin_id = (ByteBuffer) this.r_dict.get(MsgConst.ID);
			try{
				this.src_node = new Node((InetSocketAddress) datagram.getSocketAddress(), new Id(bin_id.array()));
			}
			catch (IdError e){
				throw new MsgError();
			}
			// get nodes
			ByteBuffer cnodes = (ByteBuffer) this.r_dict.get(MsgConst.NODES);
			if (cnodes != null){
				this.nodes = uncompact_nodes(cnodes);
				System.out.println(nodes.size() + " nodes");
			}
			else{
				this.nodes = new Vector<Node>();
			}
//			this.nodes2 = uncompact_nodes((ByteBuffer) this._msg_dict.get(MsgConst.NODES2));
			this.all_nodes = this.nodes;
			
			// get peers
			List<ByteBuffer> cpeers = (List<ByteBuffer>) this.r_dict.get(MsgConst.VALUES);
			if (cpeers != null){	
				this.cpeers = cpeers;//uncompact_peers(cpeers);
				System.out.println("peers");
			}
			else{
				this.cpeers = new ArrayList<ByteBuffer>(0);
			}
		}
		catch (Exception e){
//			e.printStackTrace();
			throw new MsgError();
		}
	}
	
	private Vector<Node> uncompact_nodes(ByteBuffer c_nodes) throws MsgError{
		Vector<Node> nodes = new Vector<Node>();
		byte[] cn = c_nodes.array();
		if (cn.length % 26 != 0){
			throw new MsgError();
		}
		Id id;
		Inet4Address addr;
		int port;
		int pos = 0;
		while (pos < cn.length - 1){
			try{
				id = new Id(Arrays.copyOfRange(cn, pos, pos + 20));
				pos += 20;
				addr = (Inet4Address) InetAddress.getByAddress(Arrays.copyOfRange(cn, pos, pos + 4));
				pos += 4;
				port = (cn[pos] & 0xFF) * 256 + (cn[pos + 1] & 0xFF); 
				pos += 2;
				nodes.add(new Node(new InetSocketAddress(addr, port), id));
			}
			catch (Exception e){
				e.printStackTrace();
				nodes = new Vector<Node>();
			}
		}
		return nodes;
	}
	
	private Vector<InetSocketAddress> uncompact_peers(List<ByteBuffer> c_peers) throws MsgError{
		Vector<InetSocketAddress> peers = new Vector<InetSocketAddress>();
		Inet4Address addr;
		int port;
		int pos;
		for (ByteBuffer c_peer : c_peers){
			byte[] cp = c_peer.array();
			if (cp.length % 6 != 0){
				throw new MsgError();
			}
			pos = 0;
			try{
				addr = (Inet4Address) InetAddress.getByAddress(Arrays.copyOfRange(cp, pos, pos + 4));
				pos += 4;
				port = (char)cp[pos] * 256 + (char)cp[pos + 1]; 
				pos += 2;
				peers.add(new InetSocketAddress(addr, port));
			}
			catch (Exception e){
				e.printStackTrace();
				peers = new Vector<InetSocketAddress>();
			}
		}
		return peers;
	}
	
}
