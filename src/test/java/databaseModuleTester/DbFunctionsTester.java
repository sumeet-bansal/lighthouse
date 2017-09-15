package databaseModuleTester;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.log4j.*;
import org.junit.*;

import databaseModule.DbFunctions;
import databaseModule.DirTree;
import driver.SQLiteManager;

/**
 * Tests {@link databaseModule.DbFunctions}.
 * 
 * @author ActianceEngInterns
 * @version 1.4.0
 */
public class DbFunctionsTester {

	private final static String root = System.getProperty("user.home") + "/workspace/lighthouse/root/";
	private final int PROPERTIES = 17965;

	/**
	 * Sets up the testbed by populating the SQLite database.
	 */
	@BeforeClass
	public static void setup() {

		// disables logging, works in parallel with log4j.properties
		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.OFF);
		}

		SQLiteManager.connectToDatabase();
		DbFunctions.populate(root);
	}

	/**
	 * Tests {@link databaseModule.DbFunctions#populate(java.lang.String path)}.
	 */
	@Test
	public void testPopulate() {
		SQLiteManager.clear();
		long populated = DbFunctions.populate(root);
		assertEquals(populated, PROPERTIES);
		assertEquals(SQLiteManager.getSize(), populated);
	}

	/**
	 * Tests
	 * {@link databaseModule.DbFunctions#ignore(java.lang.String location, java.util.Set properties, boolean toggle)}.
	 */
	@Test
	public void testIgnore() {

		Set<String> properties;
		Iterator<Map<String, String>> iter;
		Map<String, String> filter;
		String sql;

		// adds all property keys to HashSet
		properties = SQLiteManager.getDistinct("key", null);

		// sets all properties to not be ignored and verifies none are being ignored
		DbFunctions.ignore("", properties, false);
		filter = new HashMap<>();
		filter.put("ignore", "true");
		sql = "SELECT * FROM properties" + SQLiteManager.generateSQLFilter(filter, null) + ";";
		assertFalse(SQLiteManager.select(sql).iterator().hasNext());

		// sets all properties to be ignored and verifies all are being ignored
		DbFunctions.ignore("", properties, true);
		filter.put("ignore", "false");
		sql = "SELECT * FROM properties" + SQLiteManager.generateSQLFilter(filter, null) + ";";
		assertFalse(SQLiteManager.select(sql).iterator().hasNext());

		// sets all properties within RWC-Dev/hazelcast to be ignored and verifies
		DbFunctions.ignore("RWC-Dev/hazelcast", properties, true);
		filter = new HashMap<>();
		filter.put("environment", "RWC-Dev");
		filter.put("fabric", "hazelcast");
		filter.put("ignore", "false");
		sql = "SELECT * FROM properties" + SQLiteManager.generateSQLFilter(filter, null);
		assertFalse(SQLiteManager.select(sql).iterator().hasNext());
		properties = DbFunctions.getIgnored();
		for (String prop : properties) {
			filter = new HashMap<>();
			filter.put("key", prop);
			sql = "SELECT * FROM properties" + SQLiteManager.generateSQLFilter(filter, null);
			iter = SQLiteManager.select(sql).iterator();
			while (iter.hasNext()) {
				assertEquals(iter.next().get("ignore"), "true");
			}
		}

	}

	/**
	 * Tests {@link databaseModule.DbFunctions#popTree()}.
	 */
	@Test
	public void testPopTree() {
		DirTree tree = DbFunctions.popTree();

		// verify tree contains only and all distinct filepaths
		Iterator<String> iter = SQLiteManager.getDistinct("path", null).iterator();
		int size = tree.getSize();
		while (iter.hasNext()) {
			assertTrue(tree.hasKey(iter.next()));
			size--;
		}
		assertEquals(size, 0);
	}

}
