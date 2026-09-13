package fr.anisekai.discord.tasks.watchlist.create;

import fr.anisekai.server.domain.enums.AnimeList;

import java.util.Map;

public record WatchlistCreateTaskOutput(Map<AnimeList, Long> listMessageMap) {

}
