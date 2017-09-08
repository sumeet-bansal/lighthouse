package parserTester;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import parser.DirectoryParser;

/**
 * Tests {@link parser.DirectoryParser}.
 * 
 * @author ActianceEngInterns
 * @version 1.3.0
 */
public class DirectoryParserTester {

	private String root;
	private DirectoryParser parser;

	/**
	 * Sets up the testbed by populating the AbstractParser.
	 */
	@Before
	public void setup() {
		root = System.getProperty("user.home").replace("\\", "/") + "/workspace/lighthouse/root/";
		parser = new DirectoryParser(new File(root));
	}

	/**
	 * Tests {@link parser.DirectoryParser#findFiles(java.io.File directory)}.
	 */
	@Test
	public void testFindFiles() {
		assertEquals(parser.findFiles(new File(root)), 290);
		assertNotEquals(parser.findFiles(new File(root)), 254);
	}

	/**
	 * Tests {@link parser.DirectoryParser#parseAll()}.
	 */
	@Test
	public void testParseAll() {
		assertEquals(parser.parseAll(), 254);
		assertNotEquals(parser.parseAll(), 290);
	}

}