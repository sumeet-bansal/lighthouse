package parserTester;

import java.io.*;

import parser.*;

/**
 * Tests the Parser.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class FileParserTester {
	
	/**
	 * Runs the tester.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		File f = new File("src/hosts");
		FileParser reader = new FileParser(f);
		reader.parseFile();
		System.out.println(reader.getData());
	}
	
}