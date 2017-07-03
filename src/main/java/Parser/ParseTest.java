package parser;

import java.io.*;

/**
 * Tests the Parser.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-06-30
 */
public class ParseTest {

	/**
	 * Runs the tester.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void main(String[] args) {
		File folder = new File("C:/Users/PKelaita/Documents/ParserResources"); //file path of directory, mine is used as an example
		InputPathReader reader = new InputPathReader(folder);
		reader.parseAll();
		System.out.print(reader);
	}
}
