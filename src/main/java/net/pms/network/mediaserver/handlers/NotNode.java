package net.pms.network.mediaserver.handlers;

/**
 * Negation (NOT)
 */
class NotNode extends Node {

	Node child;

	NotNode(Node child) {
		this.child = child;
	}

	@Override
	public String toLucene() {
		String s = child.toLucene();
		return s.isEmpty() ? "" : "NOT (" + s + ")";
	}
}
