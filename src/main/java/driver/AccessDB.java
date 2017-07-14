package driver;

import cachingLayer.DbFeeder;

/**
 * Runs DbFeeder
 * 
 * @author PKelaita
 *
 */
public class AccessDB {

	public static void main(String[] args) {

		// populate database
		DbFeeder.setup();
		DbFeeder.feedDocs("C:/test root");

		// clear database
//		DbFeeder.clearDB();
	}
}
