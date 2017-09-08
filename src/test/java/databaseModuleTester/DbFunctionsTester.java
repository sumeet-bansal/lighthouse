package databaseModuleTester;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.log4j.*;
import org.bson.Document;
import org.junit.*;

import com.mongodb.client.MongoCursor;

import databaseModule.DbFunctions;
import databaseModule.DirTree;
import databaseModule.MongoManager;

/**
 * Tests {@link databaseModule.DbFunctions}.
 * 
 * @author ActianceEngInterns
 * @version 1.3.0
 */
public class DbFunctionsTester {

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
		MongoManager.connectToDatabase();
		MongoManager.clearDB();
		DbFunctions.populate(root);
	}

	/**
	 * Tests {@link databaseModule.DbFunctions#populate(java.lang.String path)}.
	 */
	@Test
	public void testPopulate() {
		DbFunctions.clearDB();
		long populated = DbFunctions.populate(root);
		assertEquals(populated, 17965);
		assertEquals(MongoManager.getCol().count(), populated);
	}

	/**
	 * Tests
	 * {@link databaseModule.DbFunctions#ignore(java.lang.String location, java.util.Set properties, boolean toggle)}
	 * and
	 * {@link databaseModule.DbFunctions#ignore(org.bson.Document filter, java.util.Set properties, boolean toggle)}.
	 */
	@Test
	public void testIgnore() {

		Set<String> properties;
		MongoCursor<Document> cursor;
		Document filter;

		// adds all property keys to HashSet
		properties = new HashSet<>();
		cursor = MongoManager.getCol().find().iterator();
		while (cursor.hasNext()) {
			properties.add(cursor.next().getString("key"));
		}

		// sets all properties to not be ignored and verifies none are being ignored
		DbFunctions.ignore(new Document(), properties, false);
		filter = new Document().append("ignore", "true");
		assertFalse(MongoManager.getCol().find(filter).iterator().hasNext());

		// sets all properties to be ignored and verifies all are being ignored
		DbFunctions.ignore(new Document(), properties, true);
		filter = new Document().append("ignore", "false");
		assertFalse(MongoManager.getCol().find(filter).iterator().hasNext());

		// sets all properties within RWC-Dev/hazelcast to be ignored and verifies
		DbFunctions.ignore("RWC-Dev/hazelcast", properties, true);
		filter = new Document().append("environment", "RWC-Dev").append("fabric", "hazelcast");
		filter.append("ignore", "false");
		assertFalse(MongoManager.getCol().find(filter).iterator().hasNext());
		properties = DbFunctions.getIgnored();
		for (String prop : properties) {
			filter = new Document().append("key", prop);
			cursor = MongoManager.getCol().find(filter).iterator();
			while (cursor.hasNext()) {
				assertEquals(cursor.next().getString("ignore"), "true");
			}
		}
		// path and matching property verification
		assertEquals(DbFunctions.ignore("BENG-Dev", properties, true), "[ERROR] Invalid path.");
		assertEquals(DbFunctions.ignore("jeremy", new HashSet<>(), true), "No matching properties found.");

	}

	/**
	 * Tests {@link databaseModule.DbFunctions#popTree()}.
	 */
	@Test
	public void testPopTree() {
		DirTree tree = DbFunctions.popTree();

		// verify tree contains only and all distinct filepaths
		MongoCursor<String> cursor = MongoManager.getCol().distinct("path", String.class).iterator();
		int size = tree.getSize();
		while (cursor.hasNext()) {
			assertTrue(tree.hasKey(cursor.next()));
			size--;
		}
		assertEquals(size, 0);
	}

}
