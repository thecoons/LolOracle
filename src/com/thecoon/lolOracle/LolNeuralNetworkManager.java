package com.thecoon.lolOracle;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

import org.bson.BsonArray;
import org.bson.Document;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCursor;

public class LolNeuralNetworkManager {
	
	LolApiManager apiMan;

	public LolNeuralNetworkManager(LolApiManager api) {
		this.apiMan = api;
	}
	
	public DataSet getDataSetFromBDD(String collectionName) {
		MongoCursor<Document> cur = this.apiMan.getDb().getCollection(collectionName).find().iterator();
		DataSet dataset = new DataSet(80,1);
		
		try {
			while (cur.hasNext()) {
				Document game = cur.next();
				Document team1 = (Document) game.get("team1");
				Document team2 = (Document) game.get("team2");
				ArrayList<Double> rowGame = new ArrayList<Double>(teamToVector(team1));
				rowGame.addAll(teamToVector(team2));
				dataset.addRow(new DataSetRow(rowGame.stream().mapToDouble(Double::doubleValue).toArray(),new double[]{game.getBoolean("team1Win?")?1:0}));
				//for(Document ply : game.get("team1"));
				
			}
		} finally {
			cur.close();
		}
		return dataset;
	}
	
	//TODO Order by Lane 
	private ArrayList<Double> teamToVector(Document team){
		ArrayList<Double> res = new  ArrayList<Double>();
		for(String fields : team.keySet()){
			System.out.println(fields);
			Document ply =  (Document) team.get(fields);
			ArrayList<Double> plystat = new ArrayList<Double>();
			for(String fieldsBis : ply.keySet()){
				if(!fieldsBis.equals("lane") && !fieldsBis.equals("id_sum")){		
					plystat.add(Double.valueOf(ply.getInteger(fieldsBis)));
				}
			}
			res.addAll(plystat);
			
		}
		return res;
		//return res.stream().mapToDouble(Double::doubleValue).toArray();
	}
	
	
	
	public static void main(String[] args) {
		LolApiManager api = new LolApiManager("48a2e66f-70a6-4b6d-af4a-bf626cb84dd3", "localhost", 27017);
		LolNeuralNetworkManager nn = new LolNeuralNetworkManager(api);
		nn.getDataSetFromBDD("TrainingData01");
		
	}

}
