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
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import org.bson.*;
import org.neuroph.core.data.DataSet;

import net.rithms.riot.api.RiotApi;
import net.rithms.riot.api.RiotApiException;
import net.rithms.riot.constant.Region;

public class LolApiManager {

	private RiotApi api;
	private MongoDatabase db;

	public LolApiManager(String apiKey, String hostMongo, Integer portMongo) {
		this.api = new RiotApi(apiKey);
		MongoClient cli = new MongoClient(hostMongo, portMongo);
		this.db = cli.getDatabase("LolOracle");
	}

	public void importGamesFromFeatureHistory(Region region, String collectionName, Integer sleepTime)
			throws RiotApiException, InterruptedException {

		try {

			// On récupére les games en vitrine
			FeaturedGames featGame = this.api.getFeaturedGames(region);

			// On parcours les games
			for (CurrentGameInfo game : featGame.getGameList()) {
				// Pour chaque joueurs
				for (Participant jrs : game.getParticipants()) {

					// On requête l'id du joueur
					Thread.sleep(sleepTime);
					Summoner sum = this.api.getSummonerByName(region, jrs.getSummonerName());
					// On requête sont historique de matchs
					Thread.sleep(sleepTime);
					MatchList matchlist = this.api.getMatchList(region, sum.getId());

					if (matchlist.getTotalGames() > 1) {
						// Pour chaque matchs
						for (MatchReference matchref : matchlist.getMatches()) {
							Boolean flag = false;
							Boolean win;
							// On filtre le smatch Ranked
							if (matchref.getQueue().equals("RANKED_SOLO_5x5")) {
								// On creer un Document pour stocjer le match
								Document docGame = new Document("game_id", String.valueOf(matchref.getMatchId()));
								Document team1 = new Document();
								Document team2 = new Document();
								// On récupére les détails du match
								Thread.sleep(sleepTime);
								MatchDetail match = this.api.getMatch(region, matchref.getMatchId());
								win = match.getTeams().get(0).isWinner();
								// Pour chaque joueurs
								List<net.rithms.riot.dto.Match.Participant> arrpart = match.getParticipants();
								Integer cpt = 0;
								for (ParticipantIdentity part : match.getParticipantIdentities()) {
									System.out.println(part.getPlayer().getSummonerId());
									// On récupére ses stats en ranked
									Document ply = playerToStatRanked(region, part, arrpart, cpt, sleepTime);
									if (ply == null) {
										flag = true;
										break;
									}
									if (arrpart.get(cpt).getTeamId() == 100) {
										team1.append(String.valueOf(arrpart.get(cpt).getChampionId()), ply);
									} else {
										team2.append(String.valueOf(arrpart.get(cpt).getChampionId()), ply);
									}
									docGame.append("team1", team1).append("team2", team2).append("team1Win?", win);
									cpt++;

								}
								if (!flag) {
									this.db.getCollection(collectionName).insertOne(docGame);
									System.out.println(docGame.toJson());
								}

								Thread.sleep(sleepTime);

							}
						}
					}
				}
			}

		} catch (RiotApiException e) {
			System.out.println(e);
		}
	}

	private Document playerToStatRanked(Region region, ParticipantIdentity part,
			List<net.rithms.riot.dto.Match.Participant> arrpart, Integer cpt, Integer timesleep)
					throws InterruptedException {
		Document ply = null;
		RankedStats plystat;
		AggregatedStats stats = new AggregatedStats();
		// TODO transformer en méthode
		try {
			Thread.sleep(timesleep);
			plystat = this.api.getRankedStats(region, part.getPlayer().getSummonerId());
			for (ChampionStats chpstat : plystat.getChampions()) {
				// Quand on match le bon champion
				if (chpstat.getId() == arrpart.get(cpt).getChampionId()) {
					// on récupére les stats du sum
					stats = chpstat.getStats();
					// On creer un fichier bson pour
					// le joueur
					ply = new Document("id_sum", String.valueOf(part.getPlayer().getSummonerId()))
							.append("lane", arrpart.get(cpt).getTimeline().getLane())
							.append("totalSession", stats.getTotalSessionsPlayed())
							.append("avg_kill", stats.getTotalChampionKills() / stats.getTotalSessionsPlayed())
							.append("avg_death", stats.getTotalDeathsPerSession() / stats.getTotalSessionsPlayed())
							.append("avg_assist", stats.getTotalAssists() / stats.getTotalSessionsPlayed())
							.append("avg_dmg_deal", stats.getTotalDamageDealt() / stats.getTotalSessionsPlayed())
							.append("avg_dmg_taken", stats.getTotalDamageTaken() / stats.getTotalSessionsPlayed())
							.append("avg_minions_kills", stats.getTotalMinionKills() / stats.getTotalSessionsPlayed())
							.append("avg_turrets_kill", stats.getTotalTurretsKilled() / stats.getTotalSessionsPlayed());

				}
			}
			return ply;

		} catch (RiotApiException e) {
			System.out.println(e);
		}
		return null;

	}

	

	public MongoDatabase getDb() {
		return db;
	}

	public void setDb(MongoDatabase db) {
		this.db = db;
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

	public static void main(String[] args) throws RiotApiException, InterruptedException {

		// RiotApi api = new RiotApi("48a2e66f-70a6-4b6d-af4a-bf626cb84dd3");
		//
		// Map<String, Summoner> summoners = api.getSummonersByName(Region.NA,
		// "rithms, tryndamere");
		// Summoner summoner = summoners.get("rithms");
		// long id = summoner.getId();
		// System.out.println(id);

		LolApiManager api = new LolApiManager("48a2e66f-70a6-4b6d-af4a-bf626cb84dd3", "localhost", 27017);
		while (true) {
			try {
				
				System.out.println("Count : "+api.db.getCollection("Test01").count());
				api.importGamesFromFeatureHistory(Region.EUW, "Test01", 3500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Thread.sleep(5000);
			} catch (RiotApiException a) {
				a.printStackTrace();
				Thread.sleep(5000);
			}
		}

	}

}
