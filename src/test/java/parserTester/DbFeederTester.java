package parserTester;

import cachingLayer.*;

/**
 * Tests the DbFeeder.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class DbFeederTester {
	
	/** TODO
	 * - actually create JUnit tests
	 */
	
	/**
	 * Runs the tester.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		DbFeeder.populate(System.getProperty("user.home") + "/workspace/diagnosticSuite/root/");
	}

}
