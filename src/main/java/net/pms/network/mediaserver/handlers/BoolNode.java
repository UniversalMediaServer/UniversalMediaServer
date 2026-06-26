package net.pms.network.mediaserver.handlers;

/**
 * A boolean operator node (AND, OR).
 */
class BoolNode extends Node {

	String op;
	Node left, right;

	BoolNode(String op, Node l, Node r) {
		this.op = op.toUpperCase();
		this.left = l;
		this.right = r;
	}

	@Override
	public String toLucene() {
		String lStr = left.toLucene();
		String rStr = right.toLucene();
		if (lStr.isEmpty()) {
			return rStr;
		}
		if (rStr.isEmpty()) {
			return lStr;
		}
		return "(" + lStr + " " + op + " " + rStr + ")";
	}
}