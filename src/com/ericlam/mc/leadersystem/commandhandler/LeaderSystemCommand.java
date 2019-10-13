package com.ericlam.mc.leadersystem.commandhandler;

import com.ericlam.mc.leadersystem.config.LeaderConfigLegacy;
import com.ericlam.mc.leadersystem.main.LeaderSystem;
import com.ericlam.mc.leadersystem.main.Utils;
import com.ericlam.mc.leadersystem.model.LeaderBoard;
import com.ericlam.mc.leadersystem.runnables.DataUpdateRunnable;
import com.hypernite.mc.hnmc.core.main.HyperNiteMC;
import com.hypernite.mc.hnmc.core.misc.commands.*;
import com.hypernite.mc.hnmc.core.misc.permission.Perm;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class LeaderSystemCommand {
    private LeaderSystem leaderSystem;
    private DefaultCommand root;


    public LeaderSystemCommand(LeaderSystem leaderSystem) {
        this.leaderSystem = leaderSystem;
        CommandNode update = new CommandNodeBuilder("update").description("強制更新排行戰績").permission(Perm.ADMIN)
                .execute((commandSender, list) -> {
                    new DataUpdateRunnable(leaderSystem).run();
                    commandSender.sendMessage(LeaderConfigLegacy.forceUpdated);
                    return true;
                }).build();

        CommandNode reload = new CommandNodeBuilder("reload").description("重載 yml").permission(Perm.ADMIN)
                .execute((commandSender, list) -> {
                    LeaderSystem.getLeaderConfigLegacy().reloadConfig();
                    commandSender.sendMessage(ChatColor.GREEN + "重載成功。");
                    return true;
                }).build();

        CommandNode get = new CommandNodeBuilder("get").description("獲得自己/別人戰績的排行與數值").permission(Perm.ADMIN)
                .placeholder("<stats> [player]")
                .execute((commandSender, list) -> {
                    Optional<LeaderBoard> leaderBoardOptional = Utils.getItem(list.get(0));
                    if (leaderBoardOptional.isEmpty()) {
                        commandSender.sendMessage(LeaderConfigLegacy.noStatistic);
                        return true;
                    }
                    LeaderBoard leaderBoard = leaderBoardOptional.get();
                    if (list.size() < 2) {
                        if (!(commandSender instanceof Player)) {
                            commandSender.sendMessage("not player");
                            return true;
                        }
                        Player player = (Player) commandSender;
                        LeaderSystem.getLeaderBoardManager().getRanking(leaderBoard).whenComplete((boardsList, ex) -> {
                            if (ex != null) {
                                ex.printStackTrace();
                                return;
                            }
                            Utils.getBoard(boardsList, player.getUniqueId()).ifPresentOrElse(board ->
                                            player.sendMessage(LeaderConfigLegacy.getStatistic.replaceAll("<item>", leaderBoard.getItem())
                                                    .replaceAll("<rank>", board.getRank() + "")
                                                    .replaceAll("<data>", board.getDataShow())),
                                    () -> player.sendMessage(LeaderConfigLegacy.notInLimit.replace("<limit>", LeaderConfigLegacy.selectLimit + "")));
                        });
                    } else {
                        String target = list.get(1);
                        LeaderSystem.getLeaderBoardManager().getRanking(leaderBoard).whenComplete((boardsList, ex) -> {
                            if (ex != null) {
                                ex.printStackTrace();
                                return;
                            }
                            Utils.getBoard(boardsList, target).ifPresentOrElse(board ->
                                            commandSender.sendMessage(LeaderConfigLegacy.getStatisticPlayer.replaceAll("<player>", target)
                                                    .replaceAll("<item>", leaderBoard.getItem())
                                                    .replaceAll("<rank>", board.getRank() + "").replaceAll("<data>", board.getDataShow())),
                                    () -> commandSender.sendMessage(LeaderConfigLegacy.notInLimit.replace("<limit>", LeaderConfigLegacy.selectLimit + "")));
                        });
                    }
                    return true;
                }).build();

        CommandNode inv = new AdvCommandNodeBuilder<Player>("inv").description("打開該戰績的排行界面").alias("openinv", "gui").placeholder("<stats>")
                .execute((player, list) -> {
                    Utils.getItem(list.get(0)).ifPresentOrElse(leaderBoard -> {
                        LeaderSystem.getLeaderInventoryManager().getLeaderInventory(leaderBoard).whenComplete((inventory, ex) -> {
                            if (ex != null) {
                                ex.printStackTrace();
                                return;
                            }
                            Bukkit.getScheduler().runTask(leaderSystem, () -> player.openInventory(inventory));
                        });
                    }, () -> player.sendMessage(LeaderConfigLegacy.noStatistic));
                    return true;
                }).build();
        this.root = new DefaultCommandBuilder("leadersystem").description("LeaderSystem 主指令").children(update, get, inv, reload).build();
    }


    public void register() {
        HyperNiteMC.getAPI().getCommandRegister().registerCommand(leaderSystem, this.root);
    }
}
