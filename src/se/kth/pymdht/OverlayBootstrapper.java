package se.kth.pymdht;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class OverlayBootstrapper {

	private List<InetSocketAddress> unstable_nodes = new ArrayList<InetSocketAddress>();
	private ArrayList<InetSocketAddress> stable_nodes = new ArrayList<InetSocketAddress>();

	public OverlayBootstrapper(BufferedReader unstable, BufferedReader stable){
		String strLine;
		String[] ip_port;
		try {
			while ((strLine = unstable.readLine()) != null)   {
//				System.out.println(strLine);
				ip_port = strLine.split(" ");
				
				this.unstable_nodes.add(new InetSocketAddress(Inet4Address.getByName(ip_port[0]), 
								Integer.parseInt(ip_port[1])));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			while ((strLine = stable.readLine()) != null)   {
//				System.out.println(strLine);
				ip_port = strLine.split(" ");
				
				this.stable_nodes.add(new InetSocketAddress(Inet4Address.getByName(ip_port[0]), 
								Integer.parseInt(ip_port[1])));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<InetSocketAddress> get_sample_unstable_addrs(int num_addrs){
		Random rnd = new Random();
		List<InetSocketAddress> sample = new ArrayList<InetSocketAddress>(num_addrs);
		for (int i = 0; i < num_addrs; i++) {
		     sample.add(this.unstable_nodes.remove(rnd.nextInt(this.unstable_nodes.size())));
		}
		return sample;
	}

    public List<InetSocketAddress> get_shuffled_stable_addrs(){
    	Collections.shuffle(stable_nodes);
        return stable_nodes;
    }
}
