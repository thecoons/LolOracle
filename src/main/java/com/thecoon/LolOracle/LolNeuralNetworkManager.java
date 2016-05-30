package com.thecoon.LolOracle;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Generated;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import org.bson.BsonArray;
import org.bson.Document;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.util.data.norm.MaxMinNormalizer;

import com.mongodb.BasicDBList;
import com.mongodb.client.MongoCursor;

public class LolNeuralNetworkManager {

	LolApiManager apiMan;

	public LolNeuralNetworkManager(LolApiManager api) {
		this.apiMan = api;
	}

	//TODO Nommer les attributs du DATASET
	public DataSet getDataSetFromBDD(String collectionName) {
		MongoCursor<Document> cur = this.apiMan.getDb().getCollection(collectionName).find().iterator();
		DataSet dataset = new DataSet(80, 1);

		try {
			while (cur.hasNext()) {
				Document game = cur.next();
				Document team1 = (Document) game.get("team1");
				Document team2 = (Document) game.get("team2");
				//ArrayList<String> attNames = getAttributsNamesOfPlayeurData(team1.get(0));
				Iterator<String> itr = team1.keySet().iterator();
				Document ply = (Document) team1.get(itr.next());
				
				ArrayList<Double> rowGame = new ArrayList<Double>(teamToVector(team1));
				rowGame.addAll(teamToVector(team2));
				dataset.addRow(new DataSetRow(rowGame.stream().mapToDouble(Double::doubleValue).toArray(),
						new double[] { game.getBoolean("team1Win?") ? 1 : 0 }));
//				ArrayList<String> att = new ArrayList<String>();
//				for(int i =0;i<10;i++)
//					att.addAll(getAttributsNamesOfPlayeurData(ply));
//				att.add("team1Win?");
//				dataset.setColumnNames(att.toArray(new String [0]));
				// for(Document ply : game.get("team1"));

			}
		} finally {
			cur.close();
		}
		return dataset;
	}

	public DataSet noramlisationOfDataSet(DataSet data) {
		MaxMinNormalizer norm = new MaxMinNormalizer();
		norm.normalize(data);
		return data;
	}

	public void exportCSV(DataSet data,String namefile) throws IOException {
		Writer fileWriter = new FileWriter(namefile);
		System.out.println(data.getInputSize() + " " +data.getOutputSize()+ " "+data.size());
		fileWriter.write(data.toCSV());
		fileWriter.close();

	}
	
	private ArrayList<String> getAttributsNamesOfPlayeurData(Document ply){
		ArrayList<String> res = new ArrayList<String>();
		for (String fieldsBis : ply.keySet()) {
			if (!fieldsBis.equals("lane") && !fieldsBis.equals("id_sum")) {
				res.add(fieldsBis);
			}
		}
		
		return res;
	}

	// TODO Order by Lane
	private ArrayList<Double> teamToVector(Document team) {
		ArrayList<Double> res = new ArrayList<Double>();
		for (String fields : team.keySet()) {
			System.out.println(fields);
			Document ply = (Document) team.get(fields);
			ArrayList<Double> plystat = new ArrayList<Double>();
			for (String fieldsBis : ply.keySet()) {
				if (!fieldsBis.equals("lane") && !fieldsBis.equals("id_sum")) {
					plystat.add(Double.valueOf(ply.getInteger(fieldsBis)));
				}
			}
			res.addAll(plystat);

		}
		return res;
		// return res.stream().mapToDouble(Double::doubleValue).toArray();
	}

	public static void main(String[] args) {
		LolApiManager api = new LolApiManager("48a2e66f-70a6-4b6d-af4a-bf626cb84dd3", "localhost", 27017);
		LolNeuralNetworkManager nn = new LolNeuralNetworkManager(api);
		DataSet data = nn.getDataSetFromBDD("TrainingData01");
		DataSet data2 = nn.getDataSetFromBDD("Test01");
		try {
			nn.exportCSV(nn.noramlisationOfDataSet(data),"train.csv");
			nn.exportCSV(nn.noramlisationOfDataSet(data2),"test.csv");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
