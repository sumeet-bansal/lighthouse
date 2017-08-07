package parser;

import java.io.*;
import java.util.*;

/**
 * Recursively takes all input files in directory and parses them according to
 * file type.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class DirectoryParser {

	private File directory;
	private ArrayList<AbstractParser> parsedData = new ArrayList<>();
	private ArrayList<String> filePaths = new ArrayList<>();

	private ArrayList<String[]> headers = new ArrayList<>();
	private ArrayList<String> errors = new ArrayList<>();

	/**
	 * Constructor.
	 * 
	 * @param directory
	 *            the root directory being parsed
	 */
	public DirectoryParser(File directory) {
		this.directory = directory;
	}

	/**
	 * Getter method for the parsed data of the files in the directory.
	 * 
	 * @return the parsed data
	 */
	public ArrayList<AbstractParser> getParsedData() {
		return parsedData;
	}

	/**
	 * Recursively searches for all files in a directory and adds their respective
	 * file paths to the appropriate internal ArrayList.
	 * 
	 * @param directory
	 *            the directory being searched
	 */
	private void fileFinder(File directory) {
		try {
			for (File file : directory.listFiles()) {
				if (file.isDirectory()) {
					fileFinder(file);
				} else {
					filePaths.add(file.getPath());
				}
			}
		} catch (NullPointerException e) {
			System.err.println("\n[DATABASE ERROR] Directory " + directory + " not found");
			return;
		}
	}

	/**
	 * Parses each file in directory and adds the resulting data to the appropriate
	 * internal ArrayList.
	 */
	public void parseAll() {
		fileFinder(directory);
		System.out.println();
		for (String path : filePaths) {
			FileParser reader = new FileParser(new File(path));
			if (reader.parseFile()) {
				parsedData.add(reader.getData());
				String[] fileWithHeader = { path, reader.getData().toString() };
				headers.add(fileWithHeader);
			} else if (reader.getErrorDescription() != null){
				System.out.println("[DATABASE MESSAGE] " + reader.getErrorDescription());
			}
		}
	}

	/**
	 * Prints each valid file in Standardizer format and lists all invalid files.
	 * 
	 * @return a String representation of DirectoryParser instance
	 */
	public String toString() {
		String str = new String();
		for (String[] fileData : headers) {
			File f = new File(fileData[0]);
			str += ("File: " + f.getName() + "\nPath: " + fileData[0] + "\nKeys:\n" + fileData[1] + "\n");
		}
		String errorList = new String();
		if (errors.size() != 0) {
			errorList += "\n";
			for (String err : errors) {
				errorList += " - " + err + "\n";
			}
		} else {
			errorList = "None\n";
		}
		str += ("Unsupported files: " + errorList + "\n");
		return str;
	}
}