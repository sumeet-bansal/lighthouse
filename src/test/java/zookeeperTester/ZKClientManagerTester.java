package zookeeperTester;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.*;

import zookeeperModule.*;

/**
 * Tests the ZkClientManager.
 */
public class ZKClientManagerTester {

	private static ZKClientManager zkmanager = new ZKClientManager("127.0.0.1");
	private static String path = "/alcatrazproperties/2.5/storm/test";
	byte[] data = "test data".getBytes();

	/**
	 * Tests zookeeperModule.ZKClientManager#create(java.lang.String, byte[]).
	 */
	@Test
	public void testCreate() {
		zkmanager.create(path, data);
		Stat stat = zkmanager.getZNodeStats(path);
		assertNotNull(stat);
		zkmanager.delete(path);
	}

	/**
	 * Tests zookeeperModule.ZKClientManager#getZNodeStats(java.lang.String).
	 */
	@Test
	public void testGetZNodeStats() throws KeeperException,
			InterruptedException {
		zkmanager.create(path, data);
		Stat stat = zkmanager.getZNodeStats(path);
		assertNotNull(stat);
		assertNotNull(stat.getVersion());
		zkmanager.delete(path);

	}

	/**
	 * Tests zookeeperModule.ZKClientManager#getZNodeData(java.lang.String).
	 */
	@Test
	public void testGetZNodeData() {
		zkmanager.create(path, data);
		String data = (String) zkmanager.getZNodeData(path, false);
		assertNotNull(data);
		zkmanager.delete(path);
	}

	/**
	 * Tests zookeeperModule.ZKClientManager#update(java.lang.String, byte[]).
	 */
	@Test
	public void testUpdate() throws KeeperException, InterruptedException {
		zkmanager.create(path, data);
		String data = "updated data";
		byte[] dataBytes = data.getBytes();
		zkmanager.update(path, dataBytes);
		String updated = (String) zkmanager.getZNodeData(path, false);
		assertNotNull(updated);
		assertEquals(updated, data);
		zkmanager.delete(path);
	}

	/**
	 * Tests zookeeperModule.ZKClientManager#getZNodeChildren(java.lang.String).
	 */
	@Test
	public void testGetZNodeChildren() {
		zkmanager.create(path, data);
		List<String> children = zkmanager.getZNodeChildren(path);
		assertNotNull(children);
		assertEquals(children.size(), 0);
		children = zkmanager.getZNodeChildren("/alcatrazproperties/2.5/storm");
		assertEquals(children.size(), 72 + 1);	// original 72 plus test ZNode
		zkmanager.delete(path);
	}

	/**
	 * Tests zookeeperModule.ZKClientManager#delete(java.lang.String).
	 */
	@Test
	public void testDelete() {
		zkmanager.create(path, data);
		Stat stat = zkmanager.getZNodeStats(path);
		zkmanager.delete(path);
		assertNotNull(stat);
		stat = zkmanager.getZNodeStats(path);
		assertNull(stat);
	}

}