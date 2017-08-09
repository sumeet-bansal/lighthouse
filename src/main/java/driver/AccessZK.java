package driver;

import java.util.*;

import zookeeperModule.*;

/**
 * Runs ZKGenerator from the command line.
 * 
 * @author ActianceEngInterns
 * @version 1.1
 */
public class AccessZK {

	private static String help = "Usage: java -jar <jar file> zk <commands>" + "\nPOSSIBLE COMMANDS \n"
			+ "'help'\n\tgoes to the help page for 'zk'\n" + "\tUsage: java -jar <jar> zk help\n"
			+ "'ls'\n\tlists ZooKeeper ZNode children for specified path within a host\n"
			+ "\tUsage: java -jar <jar> zk ls <host IP> <ZooKeeper path>\n"
			+ "'get'\n\tlists ZooKeeper ZNode contents for specified path within a host\n"
			+ "\tUsage: java -jar <jar> zk get <host IP> <ZooKeeper path>\n"
			+ "'set'\n\tsets ZooKeeper ZNode content for specified path within a host\n"
			+ "\tUsage: java -jar <jar> zk set <host IP> <ZooKeeper path> <value>\n"
			+ "'generate'\n\tgenerates .properties files from a given environment\n"
			+ "\tUsage: java -jar <jar> zk generate\n"
			+ "\t       <host IP> <ZooKeeper path> <root folder> <environment name>\n"
			+ "\t       [exception] [exception] ... [exception]";

	/**
	 * Accesses a ZooKeeper instance to generate .properties files from a given
	 * environment.
	 * 
	 * @param args
	 *            command-line arguments
	 */
	public static void run(String[] args) {

		// if no args passed, automatically sets args[0] to "help"
		if (args.length == 0) {
			args = new String[1];
			args[0] = "help";
		}

		String host, zkpath;
		ZKClientManager zkmanager;
		switch (args[0]) {
			case "help":
				System.err.println(help);
				return;
			case "generate":
	
				// necessary parameters not met
				if (args.length < 5) {
					System.err.println(help);
					return;
				}
	
				host = args[1];
				zkpath = standardizePath(args[2]);
				ZKGenerator generator = new ZKGenerator(host, zkpath, args[3], args[4]);

				// handles specified branch exceptions
				for (int i = 4; i < args.length; i++) {
					generator.addException(args[i]);
				}
				generator.generate();
	
				break;
			case "ls":
				if (args.length < 3) {
					System.err.println(help);
					return;
				}
				host = args[1];
				zkpath = standardizePath(args[2]);
	
				zkmanager = new ZKClientManager(host);
				List<String> children = zkmanager.getZNodeChildren(zkpath);
				if (children != null) {
					System.out.println(children);
				} else {
					System.err.println("Invalid file path.");
				}
	
				break;
			case "get":
				if (args.length < 3) {
					System.err.println(help);
					return;
				}
				host = args[1];
				zkpath = standardizePath(args[2]);
	
				zkmanager = new ZKClientManager(host);
				Object data = zkmanager.getZNodeData(zkpath, false);
				if (data != null) {
					System.out.println(data.toString());
				} else {
					System.err.println("Invalid file path.");
				}
	
				break;
			case "set":
				if (args.length < 4) {
					System.err.println(help);
					return;
				}
				host = args[1];
				zkpath = standardizePath(args[2]);
				byte[] value = args[3].getBytes();
	
				zkmanager = new ZKClientManager(host);
				if (zkmanager.getZNodeChildren(zkpath) != null) {
					zkmanager.update(zkpath, value);
				} else {
					System.err.println("Invalid file path.");
				}
	
				break;
			default:
				System.err.println("Invalid input. Use the 'help' command for details on usage.");
				return;
		}

	}

	private static String standardizePath(String path) {
		if (path.equals("")) {
			path = "/";
		}
		if (path.charAt(0) != '/') {
			path = "/" + path;
		}
		while (path.length() != 1 && path.charAt(path.length() - 1) == '/') {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
}