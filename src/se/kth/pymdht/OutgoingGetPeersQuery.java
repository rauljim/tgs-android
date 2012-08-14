package se.kth.pymdht;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TreeMap;

public class OutgoingGetPeersQuery {
	private Bencode _bencode;
	/*
	 */
	public OutgoingGetPeersQuery(Id src_id, Id info_hash){
		ByteBuffer version = ByteBuffer.wrap("An00".getBytes());
		TreeMap<ByteBuffer, Object> root_dict = new TreeMap<ByteBuffer, Object>();
		this._bencode = new Bencode();
		this._bencode.setRootElement(root_dict);
		root_dict.put(MsgConst.TYPE, MsgConst.QUERY);
		root_dict.put(MsgConst.QUERY, MsgConst.GET_PEERS);
		root_dict.put(MsgConst.TID, ByteBuffer.wrap("11".getBytes()));

		TreeMap<ByteBuffer, Object> args = new TreeMap<ByteBuffer, Object>();
		args.put(MsgConst.ID, ByteBuffer.wrap(src_id._bin));
		args.put(MsgConst.INFO_HASH, ByteBuffer.wrap(info_hash._bin));
		
		root_dict.put(MsgConst.ARGS, args);
	}
	
	public byte[] get_bencoded(){
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			this._bencode.print(os);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return os.toByteArray();
	}
}
