package net.pms.network.mediaserver.handlers;

/**
 * A node in the boolean expression tree. It can be either a leaf node (a search token) or a boolean operator node (AND, OR).
 */
abstract class Node {

	public abstract String toLucene();
}
