package driver;

import cachingLayer.Comparator;
import cachingLayer.DbFeeder;

public class AccessUI {

	/**
	 * Runs the comparator
	 * 
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {

		// populate database
		DbFeeder.setup();
		DbFeeder.feedDocs("C:/test root");


		// instantiate comparator with colletion
		Comparator c = new Comparator(DbFeeder.COLLECTION);

		String c1 = "Test root/*/fabric1/*/ceph.conf";
		String c2 = "Test root/*/fabric2/*/ceph.conf";
		String c3 = "Test root/*/fabric1/*/storm2.yaml";
		String c4 = "Test root/*/fabric2/*/storm2.yaml";
		String c5 = "Test root/*/fabric1/*/storm.server.properties";
		String c6 = "Test root/*/fabric2/*/storm.server.properties";
		String c7 = "Test root/*/fabric1/*/hosts";
		String c8 = "Test root/*/fabric2/*/hosts";

		c.addQuery(c3, c4);

		c.compare();

		c.writeToCSV("C:/test diffs");

		c.clearQuery();
	}
}