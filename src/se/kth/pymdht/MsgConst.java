package se.kth.pymdht;

import java.nio.ByteBuffer;

//  Copyright (C) 2009-2010 Raul Jimenez 
//  Released under GNU LGPL 2.1 
//  See LICENSE.txt for more information 

public class MsgConst {
	//  High level keys 
	public static ByteBuffer TYPE = ByteBuffer.wrap("y".getBytes());
	//  Message's type 
	public static ByteBuffer ARGS = ByteBuffer.wrap("a".getBytes());
	//  Query's arguments in a dictionary 
	public static ByteBuffer RESPONSE = ByteBuffer.wrap("r".getBytes());
	//  Reply dictionary 
	public static ByteBuffer ERROR = ByteBuffer.wrap("e".getBytes());
	//  Error message string 
	public static ByteBuffer TID = ByteBuffer.wrap("t".getBytes());
	//  Transaction ID 
	public static ByteBuffer QUERY = ByteBuffer.wrap("q".getBytes());
	//  Query command (only for queries) 
	public static ByteBuffer VERSION = ByteBuffer.wrap("v".getBytes());
	//  Client's version 
	//  Valid values for key TYPE 
//	public static byte[] QUERY = "q".getBytes();
	//  Query 
//	public static byte[] RESPONSE = "r".getBytes();
	//  Response 
//	public static byte[] ERROR = "e".getBytes();
	//  Error 
	//  Valid values for key QUERY 
	public static byte[] PING = "ping".getBytes();
	public static byte[] FIND_NODE = "find_node".getBytes();
	public static ByteBuffer GET_PEERS = ByteBuffer.wrap("get_peers".getBytes());
	public static byte[] ANNOUNCE_PEER = "announce_peer".getBytes();
	//  Valid keys for ARGS 
//	public static byte[] ID = "id".getBytes();
	//  Node's nodeID (all queries) 
	public static byte[] TARGET = "target".getBytes();
	//  Target's nodeID (find_node) 
	public static ByteBuffer INFO_HASH = ByteBuffer.wrap("info_hash".getBytes());
	//  Torrent's info_hash (get_peers and announce) 
	public static ByteBuffer PORT = ByteBuffer.wrap("port".getBytes());
	//  BitTorrent port (announce) 
//	public static byte[] TOKEN = "token".getBytes();
	//  Token (announce) 
	//  Valid keys for RESPONSE 
	public static ByteBuffer ID = ByteBuffer.wrap("id".getBytes());
	//  Node's nodeID (all replies) 
	public static ByteBuffer NODES = ByteBuffer.wrap("nodes".getBytes());
	//  String of nodes in compact format (find_nodes and get_peers) 
	public static ByteBuffer NODES2 = ByteBuffer.wrap("nodes2".getBytes());
	//  Same as previous (with IPv6 support) 
	public static ByteBuffer TOKEN = ByteBuffer.wrap("token".getBytes());
	//  Token (get_peers) 
	public static ByteBuffer PEERS = ByteBuffer.wrap("peers".getBytes());
	public static ByteBuffer VALUES = ByteBuffer.wrap("values".getBytes());
}