package se.kth.pymdht;

public class LookupNode extends Node {
	public Id distance_to_target;

	public LookupNode(Node node_, Id target){
		super(node_.addr, node_.id);
		this.distance_to_target = this.id.distance(target);
	}
}
