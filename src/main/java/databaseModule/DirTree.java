package databaseModule;

import java.util.*;

/**
 * Represents a basic directory structure as a tree.
 * 
 * @author Sumeet Bansal
 * @version 1.0
 */
public class DirTree {

	private int nelems;
	private DirNode root;

	/**
	 * Creates Nodes for the Tree.
	 * 
	 * @author Sumeet Bansal
	 * @version 1.0
	 */
	protected class DirNode {

		String name;
		HashSet<DirNode> children;

		/**
		 * Constructor for class DirNode, initializes all fields.
		 * 
		 * @param name
		 *            DirNode name
		 */
		public DirNode(String name) {
			this.name = name;
			children = new HashSet<>();
		}

		/**
		 * Getter for DirNode name.
		 * 
		 * @return the DirNode name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Getter for DirNode children.
		 * 
		 * @return the DirNode children
		 */
		public Set<DirNode> getChildren() {
			return children;
		}

		/**
		 * Adds a child to the DirNode.
		 * 
		 * @param child
		 *            the DirNode's new child
		 */
		public void addChild(DirNode child) {
			children.add(child);
		}

		/**
		 * Removes specified child node from DirNode's children.
		 * 
		 * @param child
		 *            child to be removed
		 * @return true in case of successful remove, else false
		 */
		public boolean removeChild(DirNode child) {
			return children.remove(child);
		}
	} // end of class DirNode

	/**
	 * Constructor for class DirTree, initializes empty tree with root DirNode.
	 */
	public DirTree() {
		this.nelems = 0;
		this.root = new DirNode("root");
	}

	/**
	 * Getter for the tree's root.
	 * 
	 * @return the the tree's root
	 */
	public DirNode getRoot() {
		if (nelems == 0) {
			return null;
		}
		return root;
	}

	/**
	 * Getter for the number of elements in the tree.
	 * 
	 * @return the number of elements in the tree
	 */
	public int getSize() {
		return nelems;
	}

	/**
	 * Inserts DirNode into the tree recursively.
	 * 
	 * @param path
	 *            the new path being inserted
	 * @throws NullPointerException
	 *             if the path is null
	 */
	public void insert(String path) throws NullPointerException {
		if (path == null) {
			throw new NullPointerException("The path is null.");
		}

		nelems++;
		add(root, path);
	}

	/**
	 * Determines if a certain path is in the tree.
	 * 
	 * @param path
	 *            the path of the DirNode to be found
	 * @return true if DirNode found, else false
	 * @throws NullPointerException
	 *             if the path is null
	 */
	public boolean hasKey(String path) throws NullPointerException {
		if (path == null) {
			throw new NullPointerException("The path is null.");
		}

		if (findNode(root, path) == null) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the children of the given path.
	 * 
	 * @param path
	 *            the path of the DirNode
	 * @return the children of the given DirNode
	 * @throws NullPointerException
	 *             if the path is null
	 * @throws IllegalArgumentException
	 *             if the path does not exist
	 */
	public Set<String> getChildren(String path)
			throws NullPointerException, IllegalArgumentException {
		if (path == null) {
			throw new NullPointerException("The path is null.");
		}
		if (findNode(root, path) == null) {
			throw new IllegalArgumentException("No such path.");
		}

		DirNode node = findNode(root, path);
		Set<String> children = new HashSet<>();
		for (DirNode child : node.getChildren()) {
			children.add(child.getName());
		}
		return children;
	}

	/**
	 * Helper method that finds a specific DirNode recursively.
	 * 
	 * @param curNode
	 *            the current DirNode being checked
	 * @param path
	 *            the path of the DirNode to be found
	 * @return the specified DirNode, or null
	 */
	private DirNode findNode(DirNode curNode, String path) {

		String key = "";
		if (path.indexOf("/") != -1) {
			key = path.substring(0, path.indexOf("/"));
			path = path.substring(path.indexOf("/") + 1);
		} else {
			key = path;
			path = "";
		}

		if (key.equals("")) {
			return curNode;
		}

		for (DirNode child : curNode.getChildren()) {
			if (key.equals(child.getName())) {
				return findNode(child, path);
			}
		}
		return null;
	}

	/**
	 * Helper method that adds a DirNode to the Dir recursively.
	 * 
	 * @param curNode
	 *            the current DirNode being checked
	 * @param path
	 *            the path of the DirNode to be added
	 */
	private void add(DirNode curNode, String path) {

		// base case
		if (path == null) {
			return;
		}

		// shiDirs the key and path one level down
		String key;
		if (path.indexOf("/") != -1) {
			key = path.substring(0, path.indexOf("/"));
			path = path.substring(path.indexOf("/") + 1);
		} else {
			key = path;
			path = null;
		}

		// finds correct child and goes down another level
		for (DirNode child : curNode.getChildren()) {
			if (child.getName().equals(key)) {
				add(child, path);
				return;
			}
		}

		// code block only reachable if no matching children
		DirNode child = new DirNode(key);
		curNode.addChild(child);
		add(child, path);
	}

	/**
	 * Prints a branch of the tree.
	 * @param path
	 *            the DirNode whose branch is being printed
	 * @param level
	 *            the number of levels to traverse
	 */
	public void print(String path, int level) {
		DirNode node = findNode(root, path);
		if (node == null) {
			System.err.println("[ERROR] Invalid path.");
			return;
		}
		
		String buffer = "";
		if (node != root) {
			System.out.println(node.getName());
			buffer = " | ";
		}
		for (DirNode child : node.getChildren()) {
			prnt(child, buffer, level);
		}
	}

	/**
	 * Private helper method for in-order traversal.
	 * @param node
	 *            the current DirNode
	 * @param buffer
	 *            the String buffer (used to indicate levels)
	 * @param level
	 *            the number of levels to traverse
	 */
	private void prnt(DirNode node, String buffer, int level) {
		if (level == 0) {
			return;
		}
		System.out.println(buffer + node.getName());
		for (DirNode child : node.getChildren()) {
			prnt(child, buffer + " | ", level-1);
		}
	}
}
