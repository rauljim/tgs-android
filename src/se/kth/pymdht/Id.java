package se.kth.pymdht;

import java.math.BigInteger;

public class Id implements Comparable<Id> {

	public class IdError extends Exception {
		private static final long serialVersionUID = 1L;
	}
	
	public static final int BITS_PER_BYTE = 8;
	public static final int ID_SIZE_BYTES = 20;
	public static final int ID_SIZE_BITS = (ID_SIZE_BYTES*BITS_PER_BYTE);

	public byte[] _bin;
	protected String _hex;
	protected BigInteger _long;
	public int log;
	
	public Id(String hex_id) throws IdError{
		if (hex_id.length() != ID_SIZE_BYTES * 2) {
			throw new IdError();
		}
		this._hex = hex_id;
		try{
			this._long = new BigInteger(hex_id, 16);
		}
		catch (NumberFormatException e){
			throw new IdError();
		}
		create_bin();
		create_log();
		
	}
	public Id(byte[] bin_id) throws IdError{
		if (bin_id.length != ID_SIZE_BYTES) {
			throw new IdError();
		}
		this._bin = bin_id;
		this._long = new BigInteger(this._bin);
		if (this._long.compareTo(BigInteger.ZERO) < 0){
			//negative
			this._long = this._long.add(BigInteger.valueOf(2).pow(ID_SIZE_BITS));
		}
		create_hex();
		create_log();
	}
	
	public Id(BigInteger bi){
		this._long = bi;
		create_bin();
		create_hex();
		create_log();
	}
	
	protected void create_bin(){
		byte[] unpadded_bin = this._long.toByteArray();
		int remove_prefix = 0;
		if (unpadded_bin.length > 1 && unpadded_bin[0] == 0){
			remove_prefix = 1;
			//Added by BigInteger to make it positive (two-complement)
		}
		int pad_len = ID_SIZE_BYTES - unpadded_bin.length + remove_prefix;
		this._bin = new byte[ID_SIZE_BYTES];
//		
		System.arraycopy(unpadded_bin, remove_prefix, this._bin, 
				pad_len, ID_SIZE_BYTES - pad_len);
		assert this._bin.length == ID_SIZE_BYTES;
	}
		
	protected void create_hex(){
		char[] unpadded_hex = this._long.toString(16).toCharArray();
		char[] padded_hex = new char[ID_SIZE_BYTES * 2];
		System.arraycopy(unpadded_hex, 0, padded_hex, 
				ID_SIZE_BYTES * 2 - unpadded_hex.length,
				unpadded_hex.length);
		this._hex = new String(padded_hex);
		assert this._hex.length() == ID_SIZE_BYTES * 2;
	}
	
	protected void create_log(){
		this.log = this._long.bitLength() - 1;
		if (this._long.equals(BigInteger.ZERO)){
			this.log = -1;
		}
	}

	public int compareTo(Id other){
		return this._long.compareTo(other._long);
	}
	public boolean equals(Id other){
		return this._long.equals(other._long);
	}

	public Id distance(Id other){
		/*
        Do XOR distance between two Id objects and return it as Id
        object.

		 */
		return new Id(this._long.xor(other._long));
	}
	
	public int log_distance(Id other){
		return this.distance(other).log;
	}
}