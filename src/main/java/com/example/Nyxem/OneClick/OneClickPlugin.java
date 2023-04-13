package com.example.Nyxem.OneClick;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.EthanApiPlugin.Inventory;
import com.example.EthanApiPlugin.ItemQuery;
import com.example.PacketUtilsPlugin;
import com.example.Packets.MousePackets;
import com.example.Packets.ObjectPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import com.google.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
		name = "Nyxem One Click",
		description = "Nyxem's One Clicks",
		enabledByDefault = false,
		tags = "nyxem"
)
@PluginDependency(PacketUtilsPlugin.class)
@PluginDependency(EthanApiPlugin.class)
public class OneClickPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;

	@Inject
	private OneClickConfig config;

	private boolean customItemMenuCreated = false;
	private boolean attemptedItem1on2 = false;
	int timeout = 0;

	@Override
	protected void startUp() throws Exception {
		timeout = 0;
		customItemMenuCreated = false;
		attemptedItem1on2 = false;
		log.info("Nyxem Plugin Start");
	}

	@Override
	protected void shutDown() throws Exception {
		timeout = 0;
		customItemMenuCreated = false;
		attemptedItem1on2 = false;
		log.info("Nyxem Plugin stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			log.info("logged in");
		}
	}

	@Subscribe
	private void onMenuEntryAdded(MenuEntryAdded event) {

		MenuEntry[] menuEntries = client.getMenuEntries();
		insertMenu(menuEntries);

	}

	@Subscribe
	@SneakyThrows
	public void onGameTick(GameTick tiktok) {

		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
			return;

		if (timeout > 0) {
			log.info("timeout reduction");
			timeout--;
			return;
		}

	}

	@Subscribe
	public void onClientTick(ClientTick urtiktok) {

		if (client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
			return;

		if (timeout > 0)
			return;

		boolean customItemOnItem = config.isCustomItemEnabled();
		boolean customItemOnObject = config.isCustomItemOnObjectEnabled();

		if (customItemOnItem) {

			String[] Actions = config.getCustomItemIDs().split("\n");

			for (String Action : Actions) {

				if (!Action.contains(":")) {
					log.info("no ID separator :");
					continue;
				}

				String firstItemParse = Action.split(":")[0];
				String secondItemParse = Action.split(":")[1];

				int firstItemID = Integer.parseInt(firstItemParse);
				int secondItemID = Integer.parseInt(secondItemParse);

				if (!customItemMenuCreated) {

					//log.info("trying to create menu");

					if (firstItemID == 0) {
						log.info("item 1 null");
						return;
					}
					if (secondItemID == 0) {
						log.info("item 2 null");
						return;
					}

					MenuEntry[] currentMenu = client.getMenuEntries();
					for (MenuEntry currentEntry : currentMenu) {
						if (currentEntry.getItemId() == firstItemID) {

							log.info("create menu item 1");
							MenuEntry oneClickItemMenu = client.createMenuEntry(-1)
									.setOption("One Click Item")
									.onClick(me -> ItemOnItem(firstItemID, secondItemID));

						} else if (currentEntry.getItemId() == secondItemID) {
							log.info("create menu item 2");
							MenuEntry oneClickItemMenu = client.createMenuEntry(-1)
									.setOption("One Click Item")
									.onClick(me -> ItemOnItem(secondItemID, firstItemID));
						}
					}
				} else {
					customItemMenuCreated = false;
				}
			}
		}

		if (customItemOnObject) {
			// create Item on Object stuff
		}
	}

	private void functionNameA(){

	}

	private void insertMenu(MenuEntry[] currentMenu) {

		if (customItemMenuCreated) return;

		for (MenuEntry menuEntry : currentMenu) {
			if (menuEntry.getOption().contains("One Click Item")) {
				MenuEntry[] newEntries = new MenuEntry[currentMenu.length];
				newEntries[0] = menuEntry;
				System.arraycopy(currentMenu, 0, newEntries, 1, currentMenu.length - 1);

				log.info("insert menu function");
				client.setMenuEntries(newEntries);
				customItemMenuCreated = true;
				break;
			}
		}
	}

	private void ItemOnItem(int firstID, int secondID) {

		if (!Inventory.search().withId(firstID).empty()) {
			if (!Inventory.search().withId(secondID).empty()) {

				log.info("items true, trying");
				Widget item1 = EthanApiPlugin.getItem(firstID, WidgetInfo.INVENTORY);
				Widget item2 = EthanApiPlugin.getItem(secondID, WidgetInfo.INVENTORY);

				if (item1 == null) {
					log.info("items 1 null");
					return;
				}

				if (item2 == null) {
					log.info("items 2 null");
					return;
				}

				MousePackets.queueClickPacket();
				WidgetPackets.queueWidgetOnWidget(item1, item2);
				timeout = 2;
				return;

			}
			log.info("item 2 false");
		}
		log.info("item 1 false");
		return;

	}

	@Provides
	OneClickConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(OneClickConfig.class);
	}
}
