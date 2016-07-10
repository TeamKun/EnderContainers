package fr.utarwyn.endercontainers;

import fr.utarwyn.endercontainers.commands.EnderChestCommand;
import fr.utarwyn.endercontainers.commands.EnderContainersCommand;
import fr.utarwyn.endercontainers.database.Database;
import fr.utarwyn.endercontainers.listeners.EnderChestListener;
import fr.utarwyn.endercontainers.listeners.NameTagTask;
import fr.utarwyn.endercontainers.listeners.PluginListener;
import fr.utarwyn.endercontainers.managers.DependenciesManager;
import fr.utarwyn.endercontainers.managers.EnderchestsManager;
import fr.utarwyn.endercontainers.managers.LocalesManager;
import fr.utarwyn.endercontainers.managers.MysqlManager;
import fr.utarwyn.endercontainers.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;

public class EnderContainers extends JavaPlugin {

    private static EnderContainers instance;
    private static ConfigClass configClass;
    private static Database database;

    // Managers
    public EnderchestsManager enderchestsManager;
    public DependenciesManager dependenciesManager;
    public MysqlManager mysqlManager;
    public LocalesManager localesManager;

    public NameTagTask nameTagTask = new NameTagTask();

    public String newVersion = null;
    public HashMap<String, String> newLocaleKeys = new HashMap<>();

    public void onEnable() {
        EnderContainers.instance = this;

        this.localesManager = new LocalesManager();

        loadMainConfig();
        loadBackupsConfig();
        loadCommands();
        loadManagers();
        loadListeners();
        loadTasks();

        loadMysql();
        loadMetrics();

        checkForUpdates();
    }

    public void onDisable() {

    }


    public void loadMainConfig() {
        EnderContainers.configClass = new ConfigClass(EnderContainers.getInstance());

        if (!getConfigClass().contains("main", "enabled"))
            getConfigClass().set("main", "enabled", Config.enabled);
        if (!getConfigClass().contains("main", "debug"))
            getConfigClass().set("main", "debug", Config.debug);
        if (!getConfigClass().contains("main", "prefix"))
            getConfigClass().set("main", "prefix", "&8[&6EnderContainers&8] &7");
        if (!getConfigClass().contains("main", "locale"))
            getConfigClass().set("main", "locale", Config.pluginLocale);
        if (!getConfigClass().contains("main", "enderchests.max"))
            getConfigClass().set("main", "enderchests.max", Config.maxEnderchests);
        if (!getConfigClass().contains("main", "enderchests.default"))
            getConfigClass().set("main", "enderchests.default", Config.defaultEnderchestsNumber);

        // From config.yml to locale/*.yml (from 1.0.6c version)
        if (getConfigClass().contains("main", "enderchests.mainTitle")) {
            newLocaleKeys.put("enderchest_main_gui_title", getConfigClass().getString("main", "enderchests.mainTitle"));
            getConfigClass().removePath("main", "enderchests.mainTitle");
        }else{
            newLocaleKeys.put("enderchest_main_gui_title", localesManager.getDefaultMessages().get("enderchest_main_gui_title"));
        }
        if (getConfigClass().contains("main", "enderchests.enderchestTitle")) {
            newLocaleKeys.put("enderchest_gui_title", getConfigClass().getString("main", "enderchests.enderchestTitle"));
            getConfigClass().removePath("main", "enderchests.enderchestTitle");
        }else{
            newLocaleKeys.put("enderchest_gui_title", localesManager.getDefaultMessages().get("enderchest_gui_title"));
        }

        newLocaleKeys.put("enderchest_glasspane_title", localesManager.getDefaultMessages().get("enderchest_glasspane_title"));


        // Removing from 1.0.6c version
        if (getConfigClass().contains("main", "enderchests.allowDoubleChest"))
            getConfigClass().removePath("main", "enderchests.allowDoubleChest");



        if (!getConfigClass().contains("main", "mysql.enabled"))
            getConfigClass().set("main", "mysql.enabled", Config.mysql);
        if (!getConfigClass().contains("main", "mysql.host"))
            getConfigClass().set("main", "mysql.host", Config.DB_HOST);
        if (!getConfigClass().contains("main", "mysql.port"))
            getConfigClass().set("main", "mysql.port", Config.DB_PORT);
        if (!getConfigClass().contains("main", "mysql.user"))
            getConfigClass().set("main", "mysql.user", Config.DB_USER);
        if (!getConfigClass().contains("main", "mysql.password"))
            getConfigClass().set("main", "mysql.password", Config.DB_PASS);
        if (!getConfigClass().contains("main", "mysql.database"))
            getConfigClass().set("main", "mysql.database", Config.DB_BDD);
        if (!getConfigClass().contains("main", "mysql.tablePrefix"))
            getConfigClass().set("main", "mysql.tablePrefix", Config.DB_PREFIX);

        if (!getConfigClass().contains("main", "others.blocknametag"))
            getConfigClass().set("main", "others.blocknametag", Config.blockNametag);
        if (!getConfigClass().contains("main", "others.openingChestSound"))
            getConfigClass().set("main", "others.openingChestSound", Config.openingChestSound);
        if (!getConfigClass().contains("main", "others.closingChestSound"))
            getConfigClass().set("main", "others.closingChestSound", Config.closingChestSound);
        if (!getConfigClass().contains("main", "others.updateChecker"))
            getConfigClass().set("main", "others.updateChecker", Config.updateChecker);

        Config.enabled = getConfigClass().getBoolean("main", "enabled");
        Config.debug = getConfigClass().getBoolean("main", "debug");
        Config.prefix = ChatColor.translateAlternateColorCodes('&', getConfigClass().getString("main", "prefix"));
        Config.pluginLocale = getConfigClass().getString("main", "locale");

        if (getConfigClass().getInt("main", "enderchests.max") <= 54 && getConfigClass().getInt("main", "enderchests.max") > 0)
            Config.maxEnderchests = getConfigClass().getInt("main", "enderchests.max");
        else Config.maxEnderchests = 54;
        if (getConfigClass().getInt("main", "enderchests.default") <= 54 && getConfigClass().getInt("main", "enderchests.default") >= 0)
            Config.defaultEnderchestsNumber = getConfigClass().getInt("main", "enderchests.default");
        else Config.defaultEnderchestsNumber = 1;

        Config.mysql     = getConfigClass().getBoolean("main", "mysql.enabled");
        Config.DB_HOST   = getConfigClass().getString("main", "mysql.host");
        Config.DB_PORT   = getConfigClass().getInt("main", "mysql.port");
        Config.DB_USER   = getConfigClass().getString("main", "mysql.user");
        Config.DB_PASS   = getConfigClass().getString("main", "mysql.password");
        Config.DB_BDD    = getConfigClass().getString("main", "mysql.database");
        Config.DB_PREFIX = getConfigClass().getString("main", "mysql.tablePrefix");

        Config.blockNametag      = getConfigClass().getBoolean("main", "others.blocknametag");
        Config.openingChestSound = getConfigClass().getString("main", "others.openingChestSound");
        Config.closingChestSound = getConfigClass().getString("main", "others.closingChestSound");
        Config.updateChecker     = getConfigClass().getBoolean("main", "others.updateChecker");
    }

    public void loadBackupsConfig() {
        if(!Config.mysql){
            getConfigClass().loadConfigFile("backups.yml");
            getConfigClass().loadConfigFile("players.yml");
        }
    }

    public void loadCommands() {
        getCommand("endercontainers").setExecutor(new EnderContainersCommand());
        getCommand("enderchest").setExecutor(new EnderChestCommand());
    }

    public void loadManagers() {
        this.dependenciesManager = new DependenciesManager();
        this.enderchestsManager  = new EnderchestsManager();
        this.mysqlManager        = new MysqlManager();

        this.localesManager.loadMessages();
    }

    public void loadListeners() {
        getServer().getPluginManager().registerEvents(new EnderChestListener(), this);
        getServer().getPluginManager().registerEvents(new PluginListener(), this);
    }

    public void loadTasks(){
        if(!Config.blockNametag) return;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, nameTagTask, 10L, 0);
    }

    public void loadMysql(){
        this.getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                if (Config.mysql) {
                    Config.enabled = false;

                    database = new Database();

                    mysqlManager.setDatabase(database);
                    checkDatabase();

                    if(database.isConnected()){
                        if(!database.tableExists(Config.DB_PREFIX + "enderchests")){
                            database.request("CREATE TABLE `" + Config.DB_PREFIX + "enderchests` (`id` INT(11) NOT NULL AUTO_INCREMENT, `items` MEDIUMTEXT NULL, `slots_used` INT(2) NOT NULL DEFAULT 0, `last_opening_time` TIMESTAMP NULL, `last_save_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `player_uuid` VARCHAR(36) NULL, `enderchest_id` TINYINT(2) NOT NULL DEFAULT '0', PRIMARY KEY (`id`)) COLLATE='latin1_swedish_ci' ENGINE=InnoDB;");
                            CoreUtils.log(Config.pluginPrefix + "§aMysql: table `" + Config.DB_PREFIX + "enderchests` created.");
                        }

                        if(!database.tableExists(Config.DB_PREFIX + "players")){
                            database.request("CREATE TABLE `" + Config.DB_PREFIX + "players` (`id` INT(11) NOT NULL AUTO_INCREMENT, `player_name` VARCHAR(60) NULL, `player_uuid` VARCHAR(36) NULL, `accesses` TEXT NULL, PRIMARY KEY (`id`)) COLLATE='latin1_swedish_ci' ENGINE=InnoDB;");
                            CoreUtils.log(Config.pluginPrefix + "§aMysql: table `" + Config.DB_PREFIX + "players` created.");
                        }

                        if(!database.tableExists(Config.DB_PREFIX + "backups")){
                            database.request("CREATE TABLE `" + Config.DB_PREFIX + "backups` (`id` INT(11) NOT NULL AUTO_INCREMENT, `name` VARCHAR(255) NULL, `date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, `type` VARCHAR(255) NULL, `data` MEDIUMTEXT NULL, `created_by` VARCHAR(60) NULL, PRIMARY KEY (`id`)) COLLATE='latin1_swedish_ci' ENGINE=InnoDB;");
                            CoreUtils.log(Config.pluginPrefix + "§aMysql: table `" + Config.DB_PREFIX + "backups` created.");
                        }
                    }

                    for(Player player : Bukkit.getOnlinePlayers())
                        EnderContainers.getMysqlManager().updatePlayerUUID(player);
                }else{
                    for(Player player : Bukkit.getOnlinePlayers())
                        EnderContainers.getEnderchestsManager().savePlayerInfo(player);
                }
            }
        });
    }

    public void loadMetrics(){
        try {
            Metrics metrics = new Metrics(this);
            metrics.start();
        } catch (IOException e) {
            CoreUtils.errorMessage(getServer().getConsoleSender(), "Plugin Metrics error : failed to submit stats.");
        }
    }


    public void checkForUpdates(){
        if(!Config.updateChecker) return;

        String newVersion = new Updater(this).getNewVersion();

        if(newVersion != null){
            this.newVersion = newVersion;
            CoreUtils.log(Config.pluginPrefix + "§aThere is a newer version available: §2§l" + newVersion + "§a.", true);
        }
    }



    public static EnderContainers getInstance() {
        return EnderContainers.instance;
    }

    public static EnderchestsManager getEnderchestsManager(){
        return EnderContainers.instance.enderchestsManager;
    }
    public static MysqlManager getMysqlManager(){
        return EnderContainers.instance.mysqlManager;
    }
    public static String __(String key){
        return EnderContainers.getInstance().localesManager.get(key);
    }

    public static ConfigClass getConfigClass() {
        return EnderContainers.configClass;
    }
    public static Database getDB() {
        return database;
    }

    public DependenciesManager getDependenciesManager() {
        return this.dependenciesManager;
    }


    public void reloadConfiguration() {
        EnderContainers.getConfigClass().reloadConfigs();

        loadMainConfig();
        loadBackupsConfig();

        loadMysql();
    }
    public static String getServerVersion(){
        String packageName = EnderContainers.getInstance().getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    public void checkDatabase() {
        EnderContainers.getDB().connect();
    }
    public static Boolean hasMysql(){
        return (Config.mysql && (database != null && Database.isConnected()));
    }
}
