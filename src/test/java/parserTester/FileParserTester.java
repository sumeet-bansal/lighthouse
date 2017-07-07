package parserTester;

import java.io.*;
import java.util.*;

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
		File f = new File("src/storm.server.properties");
		FileParser reader = new FileParser(f);
		reader.parseFile();
		System.out.println(reader.getData());
	}
	
}