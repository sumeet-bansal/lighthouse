package parser;

import java.io.*;
import java.util.*;

/**
 * Recursively takes all input files in directory and parses them according
 * to file type.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 * @since 2017-07-06
 */
public class DirectoryParser {
	
	private File folder;
	private ArrayList<Standardizer> parsedFiles = new ArrayList<Standardizer>();
	private ArrayList<String> filePaths = new ArrayList<>();
	private ArrayList<String[]> headers = new ArrayList<String[]>();
	private ArrayList<String> errors = new ArrayList<>();

	public DirectoryParser(File folder) {
		this.folder = folder;
	}

	/**
	 * Getter method for folderData
	 * 
	 * @return folderData
	 */
	public ArrayList<Standardizer> getParsedData() {
		return parsedFiles;
	}

	/**
	 * Searches a given directory for files and adds them to filePaths as a
	 * string representation of their paths
	 * 
	 * @param folder
	 */
	private void listFilesForFolder(File folder) {
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				filePaths.add(fileEntry.getPath());
			}
		}
	}

	/**
	 * Runs each path in filePaths through a FileInputReader and adds its
	 * standardized data to folderData and variables called in toString().
	 * Calculates runtime in milliseconds.
	 */
	public void parseAll() {
		listFilesForFolder(folder);
		for (String path : filePaths) {
			File f = new File(path);
			FileParser reader = new FileParser(f);
			reader.parseFile();
			Standardizer data = reader.getData();
			parsedFiles.add(data);
			String[] fileWithHeader = { path, data.toString() };
			headers.add(fileWithHeader);
		}
	}

	/**
	 * Prints each file in Standardizer.toString() format and specifies the file
	 * name and path. Prints a list of files failed to parse and parseAll() runtime.
	 * 
	 * @return str
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
	
	public ArrayList<String> getFilePaths() {
		return filePaths;
	}
}