package databaseModuleTester;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.log4j.*;
import org.bson.Document;
import org.junit.*;

import databaseModule.DbFunctions;
import databaseModule.MongoManager;

/**
 * Tests {@link databaseModule.MongoManager}.
 * 
 * @author ActianceEngInterns
 * @version 1.3.0
 */
public class MongoManagerTester {

	String root;

	/**
	 * Sets up the testbed by populating the Mongo database.
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
	 * Tests {@link databaseModule.MongoManager#generateFilter(java.lang.String)}
	 */
	@Test
	public void testGenerateFilter() {
		String path;
		Document expected = new Document();

		// verifies empty input
		path = "";
		expected = new Document();
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies cleaning empty input
		path = "///\\/";
		expected = new Document();
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies normal input
		path = "RWC-Dev";
		expected = new Document().append("environment", "RWC-Dev");
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies cleaning leading delimiters
		path = "/RWC-Dev";
		expected = new Document().append("environment", "RWC-Dev");
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies cleaning trailing delimiters
		path = "RWC-Dev/";
		expected = new Document().append("environment", "RWC-Dev");
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies cleaning duplicate delimiters
		path = "RWC-Dev//hazelcast";
		expected = new Document().append("environment", "RWC-Dev").append("fabric", "hazelcast");
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies cleaning duplicate delimiters for Windows
		path = "RWC-Dev\\hazelcast";
		expected = new Document().append("environment", "RWC-Dev").append("fabric", "hazelcast");
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies cleaning in general
		path = "//\\\\RWC-Dev\\\\\\//\\\\/hazelcast\\\\////\\\\";
		System.out.println(path);
		expected = new Document().append("environment", "RWC-Dev").append("fabric", "hazelcast");
		assertEquals(MongoManager.generateFilter(path), expected);

		// verifies input with extension wildcard
		path = "/RWC-Dev\\hazelcast/*/*.properties";
		expected = new Document().append("environment", "RWC-Dev").append("fabric", "hazelcast");
		expected.append("extension", "properties");
		assertEquals(MongoManager.generateFilter(path), expected);
	}

	/**
	 * Tests {@link databaseModule.MongoManager#clearDB()}.
	 */
	@Test
	public void testClearDB() {
		MongoManager.connectToDatabase();
		DbFunctions.populate(root);
		assertTrue(MongoManager.getCol().count() > 0);
		DbFunctions.clearDB();
		assertEquals(MongoManager.getCol().count(), 0);
	}

}
