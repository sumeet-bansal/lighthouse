package zookeeperModule;

import java.util.*;
import java.io.*;

/**
 * Generates .properties files for all fabrics in an environment.
 * 
 * @author ActianceEngInterns
 * @version 1.0
 */
public class ZKGenerator {
	
	private static ZKClientManager zkmanager = new ZKClientManager();
	private static String basepath = "/alcatrazproperties/2.5", server;
	private static Map<String, Object> map;
	
	/**
	 * Runs the .properties file generator.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		
		// gets all fabrics in environment
		List<String> servers = zkmanager.getZNodeChildren(basepath);
		
		for (int i = 0; i < servers.size(); i++) {
			
			// generates directory structure
			new File(servers.get(i) + "/common").mkdirs();

			// generates .properties files
			map = new LinkedHashMap<>();
			server =  servers.get(i);
			recursive(basepath + "/" + server);
			write(server, map);
		}
	}	
	
	/**
	 * Recursively traverses ZNodes to generate full path keys.
	 * @param path path of the current ZNode
	 */
	private static void recursive(String path) {
		
		// base case: current ZNode is leaf
		if (zkmanager.getZNodeStats(path).getNumChildren() == 0) {
			String key = path.replace("/", ".").substring(basepath.length() + 1);
			Object data = zkmanager.getZNodeData(path, false);
			map.put(key.substring(server.length() + 1), data);
			return;
		}
		
		// recursive case: iterates through all children
		List<String> children = zkmanager.getZNodeChildren(path);
		for (int i = 0; i < children.size(); i++) {
			recursive(path + "/" + children.get(i));
		}
	}
	
	/**
	 * Writes server data to .properties files.
	 * @param server current server
	 * @param map Map of full path keys and respective data from server
	 */
	private static void write(String server, Map<String, Object> map) {
		try {
			String directory = server + "/common/";
			Writer writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(directory + server + ".properties"), "utf-8"));
			for (Map.Entry<String, Object> entry : map.entrySet()) {
			    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
			}
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
