package driverTester;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import driver.SQLiteManager;

/**
 * Tests {@link driver.SQLiteManager}.
 * 
 * @author ActianceEngInterns
 * @version 1.4.0
 */
public class SQLiteManagerTester {

	private static final String table = SQLiteManager.getTable();
	private static Map<String, String> p1 = new HashMap<>();
	private static Map<String, String> p2 = new HashMap<>();
	private static Map<String, String> p3 = new HashMap<>();

	/**
	 * Sets up testbed by initializing test properties.
	 */
	@BeforeClass
	public static void initialize() {
		p1 = new LinkedHashMap<>();
		p1.put("key", "testk");
		p1.put("value", "testv");
		p1.put("filename", "sth2.prop");
		p1.put("node", "h2");
		p1.put("fabric", "storm");
		p1.put("environment", "Redwood-City");
		p1.put("path",
				p1.get("environment") + "/" + p1.get("fabric") + "/" + p1.get("node") + "/" + p1.get("filename"));
		p1.put("extension", "properties");
		p1.put("ignore", "false");

		p2 = new LinkedHashMap<>();
		p2.put("key", "port");
		p2.put("value", "8080");
		p2.put("filename", "esh3.prop");
		p2.put("node", "h3");
		p2.put("fabric", "elastic");
		p2.put("environment", "developer1");
		p2.put("path",
				p2.get("environment") + "/" + p2.get("fabric") + "/" + p2.get("node") + "/" + p2.get("filename"));
		p2.put("extension", "prop");
		p2.put("ignore", "false");

		p3 = new LinkedHashMap<>();
		p3.put("key", "mongo");
		p3.put("value", "db");
		p3.put("filename", "zkh1.prop");
		p3.put("node", "h1");
		p3.put("fabric", "zkepler");
		p3.put("environment", "developer8");
		p3.put("path",
				p3.get("environment") + "/" + p3.get("fabric") + "/" + p3.get("node") + "/" + p3.get("filename"));
		p3.put("extension", "prop");
		p3.put("ignore", "false");
	}

	/**
	 * Tests {@link driver.SQLiteManager#generatePathFilter(java.lang.String)}
	 */
	@Test
	public void testGeneratePathFilter() {
		String path;
		Map<String, String> expected;

		// verifies empty input
		path = "";
		expected = new HashMap<>();
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies cleaning empty input
		path = "///\\/";
		expected = new HashMap<>();
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies normal input
		path = "RWC-Dev";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies cleaning leading delimiters
		path = "/RWC-Dev";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies cleaning trailing delimiters
		path = "RWC-Dev/";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies cleaning duplicate delimiters
		path = "RWC-Dev//hazelcast";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		expected.put("fabric", "hazelcast");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies cleaning duplicate delimiters for Windows
		path = "RWC-Dev\\hazelcast";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		expected.put("fabric", "hazelcast");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies cleaning in general
		path = "//\\\\RWC-Dev\\\\\\//\\\\/hazelcast\\\\////\\\\";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		expected.put("fabric", "hazelcast");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);

		// verifies input with extension wildcard
		path = "/RWC-Dev\\hazelcast/*/*.properties";
		expected = new HashMap<>();
		expected.put("environment", "RWC-Dev");
		expected.put("fabric", "hazelcast");
		expected.put("extension", "properties");
		assertEquals(SQLiteManager.generatePathFilter(path), expected);
	}

	/**
	 * Tests {@link driver.SQLiteManager#clear()}.
	 */
	@Test
	public void testClear() {
		SQLiteManager.clear();
		ArrayList<Map<String, String>> p123 = new ArrayList<>();
		p123.add(p1);
		p123.add(p2);
		p123.add(p3);
		SQLiteManager.insertBatch(p123);
		assertTrue(SQLiteManager.getSize() > 0);
		SQLiteManager.clear();
		assertEquals(SQLiteManager.getSize(), 0);
	}

	/**
	 * Tests {@link driver.SQLiteManager#insert(java.util.Map)}.
	 */
	@Test
	public void testInsert() {
		SQLiteManager.clear();
		List<Map<String, String>> res;
		Iterator<Map<String, String>> iter;
		String sql;

		SQLiteManager.insert(p1);
		SQLiteManager.insert(p2);
		SQLiteManager.insert(p3);
		assertEquals(SQLiteManager.getSize(), 3);

		sql = "SELECT * FROM " + table + ";";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 3);
		iter = res.iterator();
		while (iter.hasNext()) {
			Map<String, String> p = iter.next();
			sql = "SELECT * FROM " + table + SQLiteManager.generateSQLFilter(p, null) + ";";
			List<Map<String, String>> r = SQLiteManager.select(sql);
			assertEquals(r.size(), 1);
		}

		sql = "SELECT * FROM " + table + " WHERE value = '8080';";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 1);
		Map<String, String> r = res.get(0);
		assertEquals(r.get("key"), p2.get("key"));
		assertEquals(r.get("filename"), p2.get("filename"));
		assertEquals(r.get("node"), p2.get("node"));
		assertEquals(r.get("fabric"), p2.get("fabric"));
		assertEquals(r.get("environment"), p2.get("environment"));
		assertEquals(r.get("extension"), p2.get("extension"));
		assertEquals(r.get("ignore"), p2.get("ignore"));
	}

	/**
	 * Tests {@link driver.SQLiteManager#update(java.util.Map, java.util.Map, java.util.Set)}.
	 */
	@Test
	public void testUpdate() {
		SQLiteManager.clear();
		List<Map<String, String>> res;
		String sql;

		SQLiteManager.insert(p1);
		SQLiteManager.insert(p2);
		SQLiteManager.insert(p3);

		sql = "SELECT * FROM " + table + " WHERE ignore = 'true';";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 0);
		Map<String, String> u = new LinkedHashMap<>();
		u.put("ignore", "true");
		Map<String, String> f = new LinkedHashMap<>();
		f.put("environment", "developer1");
		Set<String> p = new HashSet<>();
		p.add("testk");
		p.add("port");
		SQLiteManager.update(u, f, p);
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 1);
	}

	/**
	 * Tests {@link driver.SQLiteManager#delete(java.util.Map)}.
	 */
	@Test
	public void testDelete() {
		SQLiteManager.clear();
		List<Map<String, String>> res;
		String sql;

		SQLiteManager.insert(p1);
		SQLiteManager.insert(p2);
		SQLiteManager.insert(p3);

		sql = "SELECT * FROM " + table + ";";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 3);

		Map<String, String> d = new LinkedHashMap<>();
		d.put("environment", "Redwood-City");
		d.put("fabric", "storm");
		SQLiteManager.delete(d);

		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 2);
		assertEquals(res.get(0).get("key"), "port");
	}

	/**
	 * Tests {@link driver.SQLiteManager#insertBatch(Collection)}.
	 */
	@Test
	public void testInsertBatch() {
		SQLiteManager.clear();
		List<Map<String, String>> res;
		Iterator<Map<String, String>> iter;
		String sql;

		List<Map<String, String>> p123 = new ArrayList<>();
		p123.add(p1);
		p123.add(p2);
		p123.add(p3);
		SQLiteManager.insertBatch(p123);
		assertEquals(SQLiteManager.getSize(), 3);

		sql = "SELECT * FROM " + table + ";";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 3);
		iter = res.iterator();
		while (iter.hasNext()) {
			Map<String, String> p = iter.next();
			sql = "SELECT * FROM " + table + SQLiteManager.generateSQLFilter(p, null) + ";";
			List<Map<String, String>> r = SQLiteManager.select(sql);
			assertEquals(r.size(), 1);
		}

		sql = "SELECT * FROM " + table + " WHERE value = '8080';";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 1);
		Map<String, String> r = res.get(0);
		assertEquals(r.get("key"), p2.get("key"));
		assertEquals(r.get("filename"), p2.get("filename"));
		assertEquals(r.get("node"), p2.get("node"));
		assertEquals(r.get("fabric"), p2.get("fabric"));
		assertEquals(r.get("environment"), p2.get("environment"));
		assertEquals(r.get("extension"), p2.get("extension"));
		assertEquals(r.get("ignore"), p2.get("ignore"));
	}

	/**
	 * Tests {@link driver.SQLiteManager#deleteBatch(Collection)}.
	 */
	@Test
	public void testDeleteBatch() {
		SQLiteManager.clear();
		List<Map<String, String>> res;
		String sql;

		SQLiteManager.insert(p1);
		SQLiteManager.insert(p2);
		SQLiteManager.insert(p3);

		sql = "SELECT * FROM " + table + ";";
		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 3);                                                                                                                                                                          

		List<Map<String, String>> p23 = new ArrayList<>();
		p23.add(p2);
		p23.add(p3);
		SQLiteManager.deleteBatch(p23);

		res = SQLiteManager.select(sql);
		assertEquals(res.size(), 1);
		assertEquals(res.get(0).get("key"), p1.get("key"));
	}

}
