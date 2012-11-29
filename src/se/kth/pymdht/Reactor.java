package se.kth.pymdht;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import se.kth.pymdht.Controller.LookupDone;

public class Reactor {

	private static final int MAX_NUM_ROUNDS = 100;
	int timeout_delay = 100;
	private boolean _running;
	private DatagramSocket _s;
	private Controller _controller;
	private int round;
	
	public Reactor(int port, Controller controller) throws SocketException{
		this._running = false;
		this._controller = controller;
		this._s = new DatagramSocket(port);
		this._s.setReuseAddress(true);
		this._s.setSoTimeout(timeout_delay);
	}
	public void start(){
		Log.d("reactor", "start");
		assert !this._running;
		this._running = true;
		try{
			while (this._running){
				this._running = this.run_one_step();
				if (this.round > MAX_NUM_ROUNDS){
					this._running = false;
				}
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

	public boolean run_one_step(){
		List<DatagramPacket> datagrams_to_send = null;
		byte[] buf = new byte[2000];
		DatagramPacket datagram = new DatagramPacket(buf, buf.length);
		try{
			this._s.receive(datagram);
			datagrams_to_send =  _controller.on_datagram_received(datagram);
		}
		catch (SocketTimeoutException e){
			try {
				datagrams_to_send = _controller.on_heartbeat();
			} catch (LookupDone e1) {
				return false;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return true;
		}
		for(DatagramPacket datagram_to_send : datagrams_to_send){
			try{
				this._s.send(datagram_to_send);
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
		return true;
	}
}
