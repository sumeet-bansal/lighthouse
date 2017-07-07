package parser;

import java.io.*;
import java.util.ArrayList;

/**
 * Tests the Parser.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 * @since 2017-07-06
 */
public class ParserTest {
	
	/**
	 * Runs the tester.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
/*
		File f = new File("src/storm.server.properties");
		FileParser reader = new FileParser(f);
		reader.parseFile();
		System.out.println(reader.getData());
*/
		String path = "C:/Users/sbansal/Documents/parserResources";
		File folder = new File(path);
		DirectoryParser directory = new DirectoryParser(folder);
		directory.parseAll();
		ArrayList<Standardizer> parsedFiles = directory.getParsedData();
		System.out.println(parsedFiles);
	}
	
}