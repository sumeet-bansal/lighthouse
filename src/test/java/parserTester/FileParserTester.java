package parserTester;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import parser.FileParser;

/**
 * Tests {@link parser.FileParser}.
 * 
 * @author ActianceEngInterns
 * @version 1.3.0
 */
public class FileParserTester {

	private String root, path;
	private FileParser parser;

	/**
	 * Sets up the testbed by populating the AbstractParser.
	 */
	@Before
	public void setup() {
		root = System.getProperty("user.home").replace("\\", "/") + "/workspace/lighthouse/root/";
	}

	/**
	 * Tests {@link parser.FileParser#instantiateParser()}.
	 */
	@Test
	public void testInstantiateParser() {
		path = root + "RWC-Dev/storm/h2/zookeeper.cfg";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseConf");

		path = root + "RWC-Dev/storm/h2/storm.yaml";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseYaml");

		path = root + "RWC-Dev/storm/h2/storm.yaml.bkup";
		parser = new FileParser(new File(root), new File(path));
		assertNull(parser.getData());

		path = root + "RWC-Dev/kafka/h3/java.env";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseProp");

		path = root + "RWC-Dev/hazelcast/h1/apclib.jars";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseProp");

		path = root + "RWC-Dev/storm/h2/server.properties";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseProp");

		path = root + "RWC-Dev/karaf/h2/hosts";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseHosts");

		path = root + "RWC-Dev/hazelcast/h1/cluster.xml";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseXML");

		path = root + "RWC-Dev/storm/h2/product-build.info";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseInfo");

		path = root + "RWC-Dev/storm/h2/compression.whitelist";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseList");

		path = root + "RWC-Dev/storm/h2/compression.blacklist";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseList");

		path = root + "jeremy/ceph/node1/ceph.client.admin.keyring";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseCephData");

		path = root + "RWC-Dev/storm/h2/keyring.radosgw.gateway";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseCephData");

		path = root + "root.ignore";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseIgnore");

		path = root + "RWC-Dev/hazelcast/hazelcast.ignore";
		parser = new FileParser(new File(root), new File(path));
		assertEquals(parser.getData().getClass().toString(), "class parser.ParseIgnore");
	}

}