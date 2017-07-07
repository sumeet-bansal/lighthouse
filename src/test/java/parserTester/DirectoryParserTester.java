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
public class DirectoryParserTester {
	
	/**
	 * Runs the tester.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		String path = "C:/Users/sbansal/Documents/parserResources";
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<AbstractParser> parsedFiles = directory.getParsedData();
		System.out.println(parsedFiles);
	}
	
}