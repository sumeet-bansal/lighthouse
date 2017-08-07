package parserTester;

import databaseModule.*;

/**
 * Tests the DbFunctions.
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
		DbFunctions.populate(System.getProperty("user.home") + "/workspace/diagnosticSuite/root/");
	}

}
