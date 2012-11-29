package se.kth.pymdht;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import se.kth.pymdht.Id.IdError;

public class SwiftTracker{
	
	static final int HANDSHAKE = 0;
	private static final int CHANNEL_SIZE = 4;
	private static final int HASH_SIZE = 20;
	private static final int BIN_SIZE = 4;
	private static final int HASH = 4;
	static final byte[] CHANNEL_ZERO = {0,0,0,0};
	private static final boolean FAILED = false;
	private static final boolean SUCCESS = true;
	private static final byte PEX_RES = 5;
	private byte[] remote_cid;
	public Id hash = null;
	public DatagramPacket handshake_reply_datagram;
	private SocketAddress swift_addr;
	
	public SwiftTracker(){
	}
	
	public boolean on_handshake(DatagramPacket datagram){
		
		byte[] data = datagram.getData();
		this.swift_addr = datagram.getSocketAddress();
		
		if (this.hash != null){
			return FAILED;
		}
		if (datagram.getLength() < CHANNEL_SIZE + 1 + BIN_SIZE + HASH_SIZE + 1 + CHANNEL_SIZE){
			System.out.println(datagram.getLength());
			System.out.println(CHANNEL_SIZE + 1 + BIN_SIZE + HASH_SIZE + 1 + CHANNEL_SIZE);
			return FAILED;
		}
//		for (int i=0;i<datagram.getLength();i++){
//			System.out.println((int)(char)data[i]);
//		}
		int pos = 0;
		byte[] local_cid = Arrays.copyOfRange(data, pos, pos + CHANNEL_SIZE);
//		if (!local_cid.equals(CHANNEL_ZERO)){
//			System.out.println("not CHANNEL_ZERO");
//			System.out.println(local_cid[0]);
//			System.out.println(local_cid[1]);
//			System.out.println(local_cid[2]);
//			System.out.println(local_cid[3]);
//			return FAILED;
//		}
		pos += CHANNEL_SIZE;
		if (data[pos] != HASH){
			return FAILED;
		}
		pos += 1;
		pos += BIN_SIZE; //just ignore
		try {
			this.hash = new Id(Arrays.copyOfRange(data, pos, pos + HASH_SIZE));
		} catch (IdError e) {
			e.printStackTrace();
			return FAILED;
		}
		//System.out.println(this.hash._hex.length() + "hash: " + this.hash._hex);
		pos += HASH_SIZE;
		if (data[pos] != HANDSHAKE){
			System.out.println("handshake flag not found");
			return FAILED;
		}
		pos += 1;
		remote_cid = Arrays.copyOfRange(data, pos, pos + CHANNEL_SIZE);
		pos += CHANNEL_SIZE;
		
		// Ignore potential HAVEs (0x03)

		//HANDSHAKE reply
		// remote_cid HANDSHAKE local_cid 
		byte[] reply = {0,0,0,0, 0, 0,0,2,3};
		for (int i = 0; i<CHANNEL_SIZE; i++){
			//replace first bytes
			reply[i] = remote_cid[i];
		}
		try {
			handshake_reply_datagram = new DatagramPacket(reply, reply.length, datagram.getSocketAddress());
		} catch (SocketException e) {
			e.printStackTrace();
			return FAILED;
		}
		System.out.println("got valid HANDSHAKE");
		return SUCCESS;
	}
	
	public DatagramPacket get_swift_pex_datagram(List<ByteBuffer> cpeers){
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		try {
			data.write(remote_cid);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		for (ByteBuffer cpeer : cpeers){
			data.write((byte) PEX_RES);
			try {
				data.write(cpeer.array());
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			Log.d("dhttracker", "Got peer: ");
		}
		DatagramPacket datagram;
		try {
			datagram = new DatagramPacket(data.toByteArray(), data.size(), swift_addr);
		} catch (SocketException e) {
			e.printStackTrace();
			return null;
		}
		return datagram;
	}
	
}