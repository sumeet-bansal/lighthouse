package zookeeperModule;

import java.io.*;
import java.util.*;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

/**
 * Manipulates ZooKeeper nodes (ZNodes).
 * 
 * @author AlcatrazEngInterns
 * @version 1.0
 */
public class ZKClientManager {

	private static ZooKeeper zkeeper;
	private static ZKConnector zkConnection;
	private static String host = "127.0.0.1";

	/**
	 * Constructor.
	 */
	public ZKClientManager() {
		initialize();
	}

	/**
	 * Initializes ZooKeeper connection.
	 */
	private void initialize() {
		zkConnection = new ZKConnector();
		zkeeper = zkConnection.connect(host);
	}

	/**
	 * Closes the ZooKeeper connection.
	 */
	public void closeConnection() {
		zkConnection.close();
	}

	/**
	 * Creates a ZNode.
	 * @param path file path of the ZNode
	 * @param data data contained in ZNode
	 */
	public void create(String path, byte[] data) {
		try {
			zkeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
					CreateMode.PERSISTENT);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Gets the ZNode statistics.
	 * @param path the path of the ZNode
	 * @return the statistics of the ZNode
	 */
	public Stat getZNodeStats(String path) {
		Stat stat = null;
		try {
			stat = zkeeper.exists(path, true);
			if (stat != null) {
//				System.out.println("Node exists, version " + stat.getVersion());
			} else {
				System.out.println("Node does not exist.");
			}
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		return stat;
	}

	/**
	 * Gets the ZNode data.
	 * @param path the path of the ZNode
	 * @param watchFlag if a ZNode is being watched
	 * @return the data of the ZNode
	 */
	public Object getZNodeData(String path, boolean watchFlag) {
		Stat stat = null;
		try {
			stat = getZNodeStats(path);
			byte[] byteData = null;
			if (stat != null) {
				if (watchFlag){
					ZKWatcher watcher = new ZKWatcher();
					byteData = zkeeper.getData(path, watcher, null);
					watcher.await();
				} else {
					byteData = zkeeper.getData(path, null, null);
				}

				String data = new String(byteData, "UTF-8");
//				System.out.println(data);
				return data;
			} else {
				System.out.println("Node does not exist.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return stat;
	}

	/**
	 * Updates a ZNode's data.
	 * @param path the path of the ZNode to be updated
	 * @param data the data being updated in the ZNode
	 */
	public void update(String path, byte[] data) {
		try {
			int version = zkeeper.exists(path, true).getVersion();
			zkeeper.setData(path, data, version);
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Gets the paths of a ZNode's children.
	 * @param path the path of the ZNode
	 * @return the paths of the children of a ZNode
	 */
	public List<String> getZNodeChildren(String path) {
		Stat stat = getZNodeStats(path);
		List<String> children = null;

		if (stat != null) {
			try {
				children = zkeeper.getChildren(path, false);
			} catch (KeeperException | InterruptedException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < children.size(); i++) {
//				System.out.println(children.get(i));
			}
		} else {
			System.out.println("Node does not exist.");
		}
		return children;
	}
	
	/**
	 * Deletes a ZNode.
	 * @param path the path of the ZNode being deleted
	 */
	public void delete(String path) {
		try {
			int version = zkeeper.exists(path, true).getVersion();
			zkeeper.delete(path, version);
		} catch (InterruptedException | KeeperException e) {
			e.printStackTrace();
		}
	}

}