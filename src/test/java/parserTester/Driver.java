package parserTester;

import cachingLayer.Comparator;

public class Driver {

	/**
	 * Tester for comparator
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		Comparator c = new Comparator("C:/Test root");
		
		String c1 = "Test root/*/fabric1/*/ceph.conf";
		String c2 = "Test root/*/fabric2/*/ceph.conf";
		String c3 = "Test root/*/fabric1/*/storm2.yaml";
		String c4 = "Test root/*/fabric2/*/storm2.yaml";
		String c5 = "Test root/*/fabric1/*/storm.server.properties";
		String c6 = "Test root/*/fabric2/*/storm.server.properties";
		String c7 = "Test root/*/fabric1/*/hosts";
		String c8 = "Test root/*/fabric2/*/hosts";
		
		c.addQuery(c1, c2);
		c.addQuery(c7, c8);
		c.compare();
		
		c.writeToCSV("ceph_and_hosts_diffs", "C:/test diffs");
		c.clearQuery();
	}
}
