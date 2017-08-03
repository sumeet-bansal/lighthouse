package parser;

import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Standardizes .yaml files.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ParseYaml extends AbstractParser {

	/**
	 * Standardizes input File into a Map of keys and values.
	 * @param input the File to be standardized
	 */
	public void standardize(File input) {
		
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		try {

			// converts YAML file to LinkedHashMap
			@SuppressWarnings("unchecked")
			Map<String, Object> map = mapper.readValue(input, LinkedHashMap.class);
			data = new LinkedHashMap<>(map);
			
		} catch (Exception e) {
			String name = input.getAbsolutePath();
			System.err.println("\n[DATABASE ERROR] " + name + " is not in correct .yaml format.\n");
			error = true;
		}
	}

}