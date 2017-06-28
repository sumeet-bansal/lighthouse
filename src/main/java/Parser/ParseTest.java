package Parser;

import java.io.*;

/**
 * Tests the Parser.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-27
 */
public class ParseTest {
	
	/**
	 * Runs the tester.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		File f = new File("src/storm.server.properties");
		FileInputReader reader = new FileInputReader(f);
		reader.parseFile();
		System.out.println(reader.getData());
	}
	
}