package se.kth.pymdht;

import java.math.BigInteger;
import java.util.Random;


public class RandomId extends Id {
	//Create a random Id object.
	static Random rnd = new Random();
	public RandomId(){
		super(new BigInteger(Id.ID_SIZE_BITS, rnd));
	}
}
