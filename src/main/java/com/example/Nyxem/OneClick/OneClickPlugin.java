package com.example.Nyxem.OneClick;

import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.EthanApiPlugin.Inventory;
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
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Optional;

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
	int timeout = 0;

	@Override
	protected void startUp() throws Exception {
		timeout = 0;
		customItemMenuCreated = false;
		log.info("Nyxem Plugin Start");
	}

	@Override
	protected void shutDown() {
		timeout = 0;
		customItemMenuCreated = false;
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

	@SneakyThrows
	private void onMenuOptionClicked(MenuEntry entry) {

		if (!entry.getOption().equals("One Click Item")){
			log.info("Not One Click");
			return;
		}

		if (entry.getOption().equals("One Click Item")){

			log.info("One Click item clicked");

			itemOnItem(entry.getParam0(), entry.getParam1());
			return;
		}


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
	@SneakyThrows
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
						continue;
					}
					if (secondItemID == 0) {
						log.info("item 2 null");
						continue;
					}

					log.info("Log2");

					MenuEntry[] currentMenu = client.getMenuEntries();
					for (MenuEntry currentEntry : currentMenu) {
						if (currentEntry.getItemId() == firstItemID) {
							log.info("create menu item 1");
							client.createMenuEntry(-1)
									.setOption("One Click Item")
									.setType(MenuAction.CC_OP)
									.setParam0(firstItemID)
									.setParam1(secondItemID)
									.onClick(this::onMenuOptionClicked);
							return;

						} else if (currentEntry.getItemId() == secondItemID) {
							log.info("create menu item 2");
							client.createMenuEntry(-1)
									.setOption("One Click Item")
									.setType(MenuAction.CC_OP)
									.setParam0(secondItemID)
									.setParam1(firstItemID)
									.onClick(this::onMenuOptionClicked);
							return;
						}

					}
				} else {
					customItemMenuCreated = false;
				}
			}
		}

		if (customItemOnObject) {
			// create Item on Object stuff
			log.info("Log6");
		}

	}

	private void insertMenu(MenuEntry[] currentMenu) {

		if (customItemMenuCreated) return;

		for (MenuEntry menuEntry : currentMenu) {
			if (menuEntry.getOption().contains("One Click Item")) {
				log.info("Log7");
				MenuEntry tempMenu = menuEntry;
				System.arraycopy(currentMenu, 0, currentMenu, 1, currentMenu.length);
				currentMenu[0] = tempMenu;
				log.info("insert menu function");
				customItemMenuCreated = true;
				log.info("Log8");
				client.setMenuEntries(currentMenu);
				break;
			}
		}
	}

	@SneakyThrows
	private void itemOnItem(int firstID, int secondID) {

		log.info("item on item :)");
		log.info(String.valueOf(firstID));
		log.info(String.valueOf(secondID));

		Optional<Widget> item1 = Inventory.search().withId(firstID).first();
		Optional<Widget> item2 = Inventory.search().withId(secondID).first();

		log.info("widgets found");

		if (item1.isEmpty() || item2.isEmpty()){
			EthanApiPlugin.stopPlugin(this);
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Items not found.", null);
			return;
		}

		log.info(item1.toString());
		log.info(item2.toString());

		MousePackets.queueClickPacket();
		WidgetPackets.queueWidgetOnWidget(item1.get(), item2.get());
		timeout = 2;
		log.info("Log9");
	}

	@Provides
	OneClickConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(OneClickConfig.class);
	}
}
