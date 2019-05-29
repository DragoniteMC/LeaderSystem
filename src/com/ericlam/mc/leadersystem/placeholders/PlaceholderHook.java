package com.ericlam.mc.leadersystem.placeholders;

import com.ericlam.mc.leadersystem.config.ConfigManager;
import com.ericlam.mc.leadersystem.main.LeaderSystem;
import com.ericlam.mc.leadersystem.main.Utils;
import com.ericlam.mc.leadersystem.manager.LeaderBoardManager;
import com.ericlam.mc.leadersystem.model.Board;
import com.ericlam.mc.leadersystem.model.LeaderBoard;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.TreeSet;

public class PlaceholderHook extends PlaceholderExpansion {

    private final Plugin plugin;
    private final LeaderBoardManager leaderBoardManager;

    private final String NO_THIS_STATISTIC = "無此戰績";
    private final String NOT_ENOUGH_ARGS = "格式錯誤";
    private final String NOT_IN_LIMIT;

    public PlaceholderHook(LeaderSystem plugin) {
        this.plugin = plugin;
        leaderBoardManager = LeaderBoardManager.getInstance();
        NOT_IN_LIMIT = "不在前" + ConfigManager.selectLimit + "名之內";
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null) return "";
        String[] args = params.split("_");
        if (args.length != 2) return NOT_ENOUGH_ARGS;
        String item = args[1];
        Optional<LeaderBoard> leaderBoard = Utils.getItem(item);
        if (leaderBoard.isEmpty()) {
            return NO_THIS_STATISTIC;
        }
        TreeSet<Board> boardList = leaderBoardManager.getRanking(leaderBoard.get());
        switch (args[0]) {
            case "rank":
                Optional<Board> board = Utils.getBoard(boardList, p.getUniqueId());
                if (board.isEmpty()) return NOT_IN_LIMIT;
                return board.get().getRank() + "";
            case "first":
                Optional<Board> boardFirst = Utils.getBoard(boardList, 1);
                if (boardFirst.isEmpty() || boardFirst.get().getPlayerName() == null || boardFirst.get().getPlayerUUID() == null)
                    return null;
                return boardFirst.get().getPlayerName();
            default:
                break;
        }
        return null;
    }

    @Override
    public String getIdentifier() {
        return "leadersystem";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
}