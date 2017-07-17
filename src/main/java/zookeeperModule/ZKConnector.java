package zookeeperModule;

import java.io.*;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.*;
import org.apache.zookeeper.Watcher.Event.KeeperState;

/**
 * Initializes connection to ZooKeeper ensemble.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ZKConnector {

	// local instance to access ZooKeeper ensemble
	private ZooKeeper zkeeper;
	private final CountDownLatch connectionLatch = new CountDownLatch(1);

	/**
	 * Initializes the ZooKeeper connection.
	 * @param host the host being connected to
	 * @return the connected ZooKeeper instance
	 */
	public ZooKeeper connect(String host) {

		try {
			zkeeper = new ZooKeeper(host, 2000, new Watcher() {

				public void process(WatchedEvent we) {
					if (we.getState() == KeeperState.SyncConnected) {
						connectionLatch.countDown();
					}
				}
			});

			connectionLatch.await();
			return zkeeper;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		
		return zkeeper;
	}

	/**
	 * Closes the ZooKeeper connection.
	 */
	public void close() {
		try {
			zkeeper.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}