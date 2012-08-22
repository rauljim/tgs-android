package se.kth.pymdht;

class QueuedNode implements Comparable<QueuedNode>{

	public Node node;
	public Id distance;

	public QueuedNode(Node node, Id distance){
		assert distance != null;
		this.node = node;
		this.distance = distance;
	}
	
	public int compareTo(QueuedNode other){
		return this.distance.compareTo(other.distance);
	}
}