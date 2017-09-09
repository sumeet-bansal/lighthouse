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
	private final int FILES = 291;
	private final int PARSEABLE = 260;

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
		assertEquals(parser.findFiles(new File(root)), FILES);
	}

	/**
	 * Tests {@link parser.DirectoryParser#parseAll()}.
	 */
	@Test
	public void testParseAll() {
		assertEquals(parser.parseAll(), PARSEABLE);
	}

}