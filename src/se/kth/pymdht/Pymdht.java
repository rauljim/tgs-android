package se.kth.pymdht;

import java.io.BufferedReader;
import java.net.SocketException;

public class Pymdht{
	private Controller controller;
	private Reactor reactor;

	public Pymdht(int port, BufferedReader unstable, BufferedReader stable){
		this.controller = new Controller(unstable, stable);
		try {
			this.reactor = new Reactor(port, controller);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void start(){
		reactor.start();
	}
	
	public static void main(String[] args){
//		Pymdht pymdht = new Pymdht(9991);
//		pymdht.reactor.start();
	}
}