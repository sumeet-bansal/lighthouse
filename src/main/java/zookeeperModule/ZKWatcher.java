package zookeeperModule;

import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

/**
 * Watches ZooKeeper ensemble for changes.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ZKWatcher implements Watcher, StatCallback {
	
	CountDownLatch latch;

	/**
	 * Constructor.
	 */
	public ZKWatcher() {
		latch = new CountDownLatch(1);
	}

	/**
	 * Required method of Watcher interface.
	 */
	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {}

	/**
	 * Processes WatchedEvents.
	 * @param event a WatchedEvent instance
	 */
	@Override
	public void process(WatchedEvent event) {
		 System.out.println("Watcher fired on path: " + event.getPath() + " state: " + 
                 event.getState() + " type " + event.getType());
         latch.countDown();
	}
	
	/**
	 * Forces the current thread to wait until latch has counted down to zero,
	 * unless thread is interrupted.
	 */
	public void await() {
         try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
     }

}