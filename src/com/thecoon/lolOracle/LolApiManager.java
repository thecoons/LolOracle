package com.thecoon.lolOracle;

import net.rithms.riot.dto.Summoner.*;
import net.rithms.riot.dto.FeaturedGames.CurrentGameInfo;
import net.rithms.riot.dto.FeaturedGames.FeaturedGames;
import net.rithms.riot.dto.FeaturedGames.Participant;
import net.rithms.riot.dto.Game.Game;
import net.rithms.riot.dto.Match.MatchDetail;
import net.rithms.riot.dto.Match.ParticipantIdentity;
import net.rithms.riot.dto.MatchList.MatchList;
import net.rithms.riot.dto.MatchList.MatchReference;
import net.rithms.riot.dto.Stats.AggregatedStats;
import net.rithms.riot.dto.Stats.ChampionStats;
import net.rithms.riot.dto.Stats.RankedStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;

import org.bson.*;

import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.constant.Region;

public class LolApiManager {

	private RiotApi api;
	private MongoDatabase db;

	public LolApiManager(String apiKey,String hostMongo,Integer portMongo) {
		this.api = new RiotApi(apiKey);
		MongoClient cli = new MongoClient(hostMongo,portMongo);
		this.db = cli.getDatabase("LolOracle");
	}

	public void importGamesFromFeatureHistory(Region region,String collectionName) throws RiotApiException, InterruptedException {
		//On récupére les games en vitrine
		FeaturedGames featGame = this.api.getFeaturedGames(region);
		
		//On parcours les games
		for (CurrentGameInfo game : featGame.getGameList()) {
			//Pour chaque joueurs
			for (Participant jrs : game.getParticipants()) {
				//On requête l'id du joueur
				Summoner sum = this.api.getSummonerByName(region, jrs.getSummonerName());
				//On requête sont historique de matchs
				MatchList matchlist = this.api.getMatchList(region, sum.getId());
				
				if (matchlist.getTotalGames() > 1) {
					//Pour chaque matchs
					for (MatchReference matchref : matchlist.getMatches()) {
						//On filtre le smatch Ranked
						if (matchref.getQueue().equals("RANKED_SOLO_5x5")) {
							//On creer un Document pour stocjer le match
							Document docGame = new Document("game_id", String.valueOf(matchref.getMatchId()));	
							Document team1 = new Document();
							Document team2 = new Document();
							//On récupére les détails du match
							MatchDetail match = this.api.getMatch(region,matchref.getMatchId());
							//Pour chaque joueurs
							List<net.rithms.riot.dto.Match.Participant> arrpart = match.getParticipants();
							Integer cpt = 0;
							for(ParticipantIdentity part : match.getParticipantIdentities()){
								//On récupére ses stats en ranked
								Thread.sleep(1000);
								System.out.println(part.getPlayer().getSummonerId());
								RankedStats plystat;
								AggregatedStats stats = new AggregatedStats();
								try{
								 plystat = this.api.getRankedStats(region, part.getPlayer().getSummonerId());
									for (ChampionStats chpstat : plystat.getChampions()) {
										//Quand on match le bon champion
										if(chpstat.getId() == arrpart.get(cpt).getChampionId() ){
											//on récupére les stats du sum
											 stats = chpstat.getStats();
											//On creer un fichier bson pour le joueur
												//TODO transformer en méthode
												Document ply = new Document("id_sum",String.valueOf(part.getPlayer().getSummonerId()))
														.append("champ_id", arrpart.get(cpt).getChampionId())
														.append("totalSession", stats.getTotalSessionsPlayed())
														.append("avg_kill", stats.getTotalChampionKills()/stats.getTotalSessionsPlayed())
														.append("avg_death", stats.getTotalDeathsPerSession()/stats.getTotalSessionsPlayed())
														.append("avg_assist", stats.getTotalAssists()/stats.getTotalSessionsPlayed())
														.append("avg_dmg_deal", stats.getTotalDamageDealt()/stats.getTotalSessionsPlayed())
														.append("avg_dmg_taken", stats.getTotalDamageTaken()/stats.getTotalSessionsPlayed())
														.append("avg_minions_kills", stats.getTotalMinionKills()/stats.getTotalSessionsPlayed())
														.append("avg_turrets_kill", stats.getTotalTurretsKilled()/stats.getTotalSessionsPlayed());
												
												if(arrpart.get(cpt).getTeamId() == 100){
													team1.append(arrpart.get(cpt).getTimeline().getLane(), ply);
												}else{
													team2.append(arrpart.get(cpt).getTimeline().getLane(), ply);
												}
												docGame.append("team1", team1).append("team2", team2);
										}
									}
									
								}catch(RiotApiException e){
									System.out.println(e);
									
								}
								
						
								cpt++;
								Thread.sleep(2000);
								
							}
							
							System.out.println(docGame.toJson());
							
						}
					}
				}
			}
		}

	}

	public RiotApi getApi() {
		return api;
	}

	public void setApi(RiotApi api) {
		this.api = api;
	}

	public FeaturedGames getFeacturedGames(Region region) throws RiotApiException {
		return this.api.getFeaturedGames(region);
	}

	public static void main(String[] args) throws RiotApiException {

		// RiotApi api = new RiotApi("48a2e66f-70a6-4b6d-af4a-bf626cb84dd3");
		//
		// Map<String, Summoner> summoners = api.getSummonersByName(Region.NA,
		// "rithms, tryndamere");
		// Summoner summoner = summoners.get("rithms");
		// long id = summoner.getId();
		// System.out.println(id);

		LolApiManager api = new LolApiManager("48a2e66f-70a6-4b6d-af4a-bf626cb84dd3","localhost",27017);
		try {
			api.importGamesFromFeatureHistory(Region.EUW,"TrainingData01");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
