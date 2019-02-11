package com.ericlam.mc.manager;

import com.caxerx.builders.InventoryBuilder;
import com.caxerx.builders.ItemStackBuilder;
import com.ericlam.mc.config.ConfigManager;
import com.ericlam.mc.main.Utils;
import com.ericlam.mc.model.Board;
import com.ericlam.mc.model.LeaderBoard;
import com.hypernite.skin.PlayerHeadGetter;
import com.hypernite.skin.SkinDatabaseManager;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class LeaderInventoryManager {
    private static LeaderInventoryManager leaderInventoryManager;
    private LeaderBoardManager leaderBoardManager;
    private HashMap<String, Inventory> leaderInventories = new HashMap<>();
    private BukkitTask updateTask;

    private LeaderInventoryManager() {
        leaderBoardManager = LeaderBoardManager.getInstance();
    }

    public static LeaderInventoryManager getInstance() {
        if (leaderInventoryManager == null) leaderInventoryManager = new LeaderInventoryManager();
        return leaderInventoryManager;
    }

    public HashMap<String, Inventory> getLeaderInventories() {
        return leaderInventories;
    }

    private String replaceData(String str, Board board) {
        return str.replaceAll("<player>", board.getPlayerName()).replaceAll("<rank>", board.getRank() + "").replaceAll("<data>", board.getDataShow());
    }

    private Inventory getLeaderInventoryFromSQL(LeaderBoard leaderBoard) {
        String item = leaderBoard.getItem();
        Inventory inv = new InventoryBuilder(ConfigManager.guiSize, leaderBoard.getInvTitle()).build();
        List<Board> boards = leaderBoardManager.getRanking(leaderBoard);
        for (int i = 0; i < ConfigManager.guiSize; i++) {
            Board board = boards.get(i);
            if (board.getPlayerName() == null || board.getPlayerUUID() == null) continue;
            String b64 = SkinDatabaseManager.getInstance().getPlayerSkin(board.getPlayerUUID(), board.getPlayerName());
            String invName = replaceData(leaderBoard.getInvName(), board);
            ItemStack stack = new ItemStackBuilder(Material.PLAYER_HEAD).displayName(invName)
                    .lore(leaderBoard.getLores().stream().map(line -> replaceData(line, board)).collect(Collectors.toList())).build(); //onClick method have bug, wait caxerx to fix it
            PlayerHeadGetter.setSkullMeta(b64, stack);
            inv.setItem(i, stack);
        }
        leaderInventories.put(item, inv);
        return inv;
    }


    public Inventory getLeaderInventory(LeaderBoard leaderBoard) {
        String item = leaderBoard.getItem();
        if (leaderInventories.containsKey(item)) return leaderInventories.get(item);
        else return getLeaderInventoryFromSQL(leaderBoard);
    }


    public void forceUpdateInv() {
        ConcurrentLinkedDeque<String> itemQueue = new ConcurrentLinkedDeque<>(leaderInventories.keySet());
        synchronized (this) {
            while (!itemQueue.isEmpty()) {
                String item = itemQueue.poll();
                if (item == null) continue;
                LeaderBoard leaderBoard = Utils.getItem(item);
                if (leaderBoard != null) getLeaderInventoryFromSQL(leaderBoard);
            }
        }
    }

    public void inventoryUpdateScheduler(Plugin plugin) {
        if (updateTask != null) return;
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                forceUpdateInv();
                plugin.getLogger().info("Leader Inventories Updated.");
            }
        }.runTaskTimerAsynchronously(plugin, 300 * 20L, 3600 * 20L);
    }


}