package parserTester;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

import parser.AbstractParser;
import parser.ParseProp;

/**
 * Tests {@link parser.AbstractParser}.
 * 
 * @author ActianceEngInterns
 * @version 1.3.0
 */
public class AbstractParserTester {

	private String root, path;
	private AbstractParser parser;

	/**
	 * Sets up the testbed by populating the AbstractParser.
	 */
	@Before
	public void setup() {
		root = System.getProperty("user.home").replace("\\", "/") + "/workspace/lighthouse/root/";
		path = root + "jeremy/storm/common/server.properties";
		parser = new ParseProp();
		parser.setPath(root, path);
		parser.standardize(new File(path));
	}

	/**
	 * Tests {@link parser.AbstractParser#isInternal()}.
	 */
	@Test
	public void testIsInternal() {
		assertFalse(parser.isInternal());
	}

	/**
	 * Tests {@link parser.AbstractParser#getMetadata()}.
	 */
	@Test
	public void testGetMetadata() {
		Map<String, String> metadata = parser.getMetadata();
		assertEquals(metadata.get("path"), "jeremy/storm/common/server.properties");
		assertNotEquals(metadata.get("path"), "chris/storm/common/server.properties");
		assertNull(metadata.get("key"));
		assertEquals(metadata.get("environment"), "jeremy");
		assertEquals(metadata.get("fabric"), "storm");
		assertEquals(metadata.get("node"), "common");
		assertEquals(metadata.get("filename"), "server.properties");
		assertEquals(metadata.get("extension"), "properties");
		parser.setPath(root, root + "chris/storm/common/server.properties");
		metadata = parser.getMetadata();
		assertEquals(metadata.get("path"), "chris/storm/common/server.properties");
		assertNotEquals(metadata.get("path"), "jeremy/storm/common/server.properties");
		assertNull(metadata.get("key"));
		assertEquals(metadata.get("environment"), "chris");
		assertEquals(metadata.get("fabric"), "storm");
		assertEquals(metadata.get("node"), "common");
		assertEquals(metadata.get("filename"), "server.properties");
		assertEquals(metadata.get("extension"), "properties");
	}

	/**
	 * Tests {@link parser.AbstractParser#getData()}.
	 */
	@Test
	public void testGetData() {
		Map<String, Object> data = parser.getData();
		assertEquals(data.get("reindex/mongo/write/batchsize"), "50");
		assertEquals(data.get("server/archive/es/host/ip"), "127.0.0.1");
		assertEquals(data.get("es/settings/archive/cluster/name"), "elasticsearch");
		assertNotEquals(data.get("reindex/mongo/write/batchsize"), 50);
		assertNotEquals(data.get("server/archive/es/host/ip"), new Integer[] { 127, 0, 0, 2 });
		assertNotEquals(data.get("es/settings/archive/cluster/name"), "googlesearch");
	}

	/**
	 * Tests {@link parser.AbstractParser#clear()}.
	 */
	@Test
	public void testClear() {
		assertNotEquals(parser.getData().size(), 0);
		parser.clear();
		assertEquals(parser.getData().size(), 0);
	}

}