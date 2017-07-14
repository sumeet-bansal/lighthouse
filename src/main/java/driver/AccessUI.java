package driver;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import cachingLayer.Comparator;

public class AccessUI {

	/**
	 * Runs the comparator
	 * 
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {

		// pull collection from database
		@SuppressWarnings("resource")
		MongoClient client = new MongoClient("localhost", 27017);
		MongoDatabase database = client.getDatabase("ADS_DB");
		MongoCollection<Document> col = database.getCollection("ADS_COL");

		// instantiate comparator with colletion
		Comparator c = new Comparator(col);

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
		c.writeToCSV("C:/test diffs");
		c.clearQuery();
	}
}