package cachingLayer;

import java.util.List;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class MongoMethods {

	public static void main( String args[] ) {

		try {
			
			// connecting with server
			@SuppressWarnings("resource")
			MongoClient mongoClient = new MongoClient("localhost", 27017);
			System.out.println("server connection successfully done");
			
			// list all Databases
			@SuppressWarnings("deprecation")
			List<String> databases = mongoClient.getDatabaseNames();
			System.out.println(databases);
			
			// connecting with Database
			MongoDatabase dbs = mongoClient.getDatabase("test");
			System.out.println("connected to database " + dbs.getName());
			
			// create Collection
			String colName = "ahos";
			MongoCollection<Document> col = dbs.getCollection(colName);
			col.drop(); dbs.createCollection(colName);
			// String colName = col.getNamespace().getCollectionName();
			System.out.println("created collection " + colName);

			// insert Document in Collection
			Document doc1 = new Document().
					append("name", "Sumeet").
					append("age", 19).
					append("uni", "ucsd");
			Document doc2 = new Document().
					append("name", "Gagan").
					append("age", 17).
					append("hs", "basis");
			Document doc3 = new Document().
					append("name", "Pierce").
					append("age", 18).
					append("uni", "u dub");
			Document doc4 = new Document().
					append("name", "Cameron").
					append("age", 19).
					append("uni", "brown");
			Document doc5 = new Document().
					append("name", "Sara").
					append("age", 22).
					append("uni", "usc");
			col.insertOne(doc1);
			col.insertOne(doc2);
			col.insertOne(doc3);
			col.insertOne(doc4);
			col.insertOne(doc5);
			System.out.println("inserted docs in collection " + colName);

			// read or find Document from Database
			Document filter = new Document()
					.append("age", 19);
			FindIterable<Document> iter = col.find(filter);
			MongoCursor<Document> cursor = iter.iterator();
			while (cursor.hasNext()) {
				System.out.println(cursor.next()); 
			}

			// update Document in Collection
			Document updatedDoc = new Document();
			updatedDoc.append("$set", new Document().append("uni","n/a"));
			Document oldDoc = new Document().append("name", "Sara");
			col.updateOne(oldDoc, updatedDoc);
			System.out.println("updated doc");
			
			filter = new Document().append("name", "Sara");
			iter = col.find(filter);
			cursor = iter.iterator();
			while (cursor.hasNext()) {
				System.out.println(cursor.next()); 
			}
			
			System.out.println("all docs");
			cursor = col.find().iterator();
			while (cursor.hasNext()) {
				System.out.println(cursor.next());
			}

			// delete Document
			Document del = new Document("name", "Cameron");
			col.deleteOne(del);
			del = new Document("name", "Sara");
			col.deleteOne(del);
			System.out.println("deleted Sara and Cameron");
			cursor = col.find().iterator();
			while (cursor.hasNext()) {
				System.out.println(cursor.next());
			}
/*
			// list all Collections
			List<String> cols = dbs.listCollectionNames();
			System.out.println(cols);
*/	
			// drop Collection
			col.drop();
			System.out.println("dropped collection");
			
			// drop Database
			// dbs.drop();
			// System.out.println("dropped database");

		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
		}
	}

}
