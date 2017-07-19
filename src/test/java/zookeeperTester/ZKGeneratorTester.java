package zookeeperTester;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;
import org.junit.*;

import parser.*;
import zookeeperModule.*;

/**
 * Tests the ZkGenerator.
 */
public class ZKGeneratorTester {

	private ZKClientManager zkmanager = new ZKClientManager();
	private ArrayList<AbstractParser> parsedFiles;
	private String zkpath = "/alcatrazproperties/2.5/";
	private String localf = "C:/Users/sbansal/workspace/diagnosticSuite/environment/";

	/**
	 * Sets up testing suite.
	 */
	@Before
	public void setup() {
		ZKGenerator generator = new ZKGenerator(); generator.generate();
		DirectoryParser directory = new DirectoryParser(new File(localf));
		directory.parseAll();
		parsedFiles = directory.getParsedData();
	}

	/**
	 * Tests zookeeperModule.ZKGenerator#generate().
	 */
	@Test
	public void testGenerate() {
		for (AbstractParser s : parsedFiles) {
			Map<String, String> metadata = s.getMetadata();
			String fabric = metadata.get("fabric");

			// follows key paths from generated .properties in ZooKeeper server
			// and checks file values against actual ZooKeeper values
			Map<String, Object> data = s.getData();
			for (Map.Entry<String, Object> entry : data.entrySet()) {
				String path = zkpath + fabric + "/"+ entry.getKey();
				String zkprop = (String) zkmanager.getZNodeData(path, false);
				String parsed = (String) entry.getValue();
				assertEquals(zkprop, parsed);
			}

			// logs file as 'validated' if assertion passed for all properties
			metadata.remove("path");
			String path = "";
			for (Map.Entry<String, String> entry : metadata.entrySet()) {
				path = "/" + entry.getValue() + path;
			}
			System.out.println("validated " + path);
		}
	}

}