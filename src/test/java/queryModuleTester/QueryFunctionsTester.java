package queryModuleTester;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.log4j.*;
import org.junit.*;

import databaseModule.DbFunctions;
import queryModule.QueryFunctions;
import driver.SQLiteManager;

/**
 * Tests {@link queryModule.QueryFunctions}.
 * 
 * @author ActianceEngInterns
 * @version 1.4.0
 */
public class QueryFunctionsTester {

	String root;

	/**
	 * Sets up the testbed by populating the SQLite database.
	 */
	@Before
	public void setup() {

		// disables logging, works in parallel with log4j.properties
		@SuppressWarnings("unchecked")
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.OFF);
		}

		root = System.getProperty("user.home") + "/workspace/lighthouse/root/";
	}

	/**
	 * Tests {@link queryModule.QueryFunctions#grep(java.lang.String, int)}.
	 */
	@Test
	public void testGrep() {
		SQLiteManager.connectToDatabase();
		SQLiteManager.clear();
		DbFunctions.populate(root);
		Set<String> expected;

		// verifies grep, no toggle
		expected = new HashSet<>();
		expected.add("lfs/ingestion/large-file/chunk/size");
		expected.add("lfs/ingestion/large-file-event/min/size");
		expected.add("lfs/ingestion/topics");
		expected.add("lfs/ingestion/consumer/group");
		assertEquals(QueryFunctions.grep("lfs/ingestion", 0), expected);

		// verifies grep, toggled
		expected = new HashSet<>();
		expected.add("/data/elastic/data");
		expected.add("/data/elastic/logs");
		expected.add("elasticsearch");
		expected.add("org.elasticsearch.action");
		expected.add("org.elasticsearch.deprecation");
		assertEquals(QueryFunctions.grep("elastic", 1), expected);

		// verifies grep, toggled
		expected = new HashSet<>();
		expected.add("eng02essreports");
		expected.add("report");
		assertEquals(QueryFunctions.grep("report", 1), expected);

		// verifies grep with no results, no toggle
		assertEquals(QueryFunctions.grep("true", 0), new HashSet<>());
		assertEquals(QueryFunctions.grep("false", 0), new HashSet<>());
	}

	/**
	 * Tests
	 * {@link queryModule.QueryFunctions#findProp(java.lang.String, java.lang.String, int)}.
	 */
	@Test
	public void testFindProp() {
		String expected;
		String location;

		// verifies find, no toggle and no location
		expected = "5000000";
		for (Map<String, String> found : QueryFunctions.findProp("lgs/ingestion/large-file/chunk/size", null, 0)) {
			assertEquals(found.get("value"), expected);
		}

		// verifies find, toggled but no location
		expected = "es.settings.metrics.cluster.name";
		for (Map<String, String> found : QueryFunctions.findProp("eng02essreports", null, 1)) {
			assertEquals(found.get("key"), expected);
		}

		// verifies find, no toggle but with location
		expected = "8888";
		location = "jeremy/karaf/common/server.properties";
		assertEquals(QueryFunctions.findProp("report/port", "jeremy", 0).get(0).get("value"), expected);
		assertEquals(QueryFunctions.findProp("report/port", "jeremy", 0).get(0).get("path"), location);

		// verifies find, toggled and with location
		expected = "server.primary.es.host.cluster.name";
		location = "RWC-Dev/karaf/h1/bamboo.server.properties";
		assertEquals(QueryFunctions.findProp("elasticsearch", "RWC-Dev", 1).get(0).get("key"), expected);
		assertEquals(QueryFunctions.findProp("elasticsearch", "RWC-Dev", 1).get(0).get("path"), location);
	}

}
