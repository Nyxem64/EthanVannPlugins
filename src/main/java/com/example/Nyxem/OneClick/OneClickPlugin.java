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
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;

import java.util.Objects;
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

	@Inject
	private ItemManager itemManager;

	private boolean customItemMenuCreated = false;
	private int timeout = 0;

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

	private void onMenuOptionClicked(String name, int firstID, int secondID) {

		customItemMenuCreated = false;

		if (!name.contains("One Click Item")){
			log.info("Not One Click");
		}else {
			log.info("One Click item clicked");
			itemOnItem(firstID, secondID);
			return;
		}

		if (!name.contains("One Click Object")){
			log.info("Not One Click Object");
		}else {
			log.info("One Click object clicked");
			return;
		}

	}

	@Subscribe
	public void onGameTick(GameTick tiktok) {

		if (timeout > 0) {
			log.info("timeout reduction");
			timeout--;
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

				if (customItemMenuCreated)
					break;

				if (!Action.contains(":")) {
					log.info("no ID separator :");
					continue;
				}

				String firstItemParse = Action.split(":")[0];
				String secondItemParse = Action.split(":")[1];

				int firstItemID = Integer.parseInt(firstItemParse);
				int secondItemID = Integer.parseInt(secondItemParse);

				if (Inventory.search().withId(firstItemID).first().isEmpty()){
					//client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","Item 1 not found in inventory","");
					continue;
				}

				if (Inventory.search().withId(secondItemID).first().isEmpty()){
					//client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","Item 2 not found in inventory","");
					continue;
				}

				MenuEntry[] currentMenu = client.getMenuEntries();
				String optionName = "One Click Item ";
				for (MenuEntry currentEntry : currentMenu) {

					if (currentEntry.getItemId() != firstItemID && currentEntry.getItemId() != secondItemID){
						continue;
					}

					log.info("current entry is item");

					client.createMenuEntry(-1)
							.setOption(optionName + itemManager.getItemComposition(firstItemID).getName() + " -> " + itemManager.getItemComposition(secondItemID).getName())
							.setType(MenuAction.CC_OP)
							.onClick(me -> onMenuOptionClicked(optionName, firstItemID, secondItemID));
					customItemMenuCreated = true;
					break;
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
				MenuEntry tempMenu = menuEntry;
				System.arraycopy(currentMenu, 0, currentMenu, 1, currentMenu.length);
				currentMenu[0] = tempMenu;
				log.info("insert menu function");
				customItemMenuCreated = true;
				client.setMenuEntries(currentMenu);
				break;
			}
		}
	}

	private void itemOnItem(int firstID, int secondID) {


		Optional<Widget> item1 = Inventory.search().withId(firstID).first();
		Optional<Widget> item2 = Inventory.search().withId(secondID).first();

		if(item1.isEmpty() || item2.isEmpty())
			return;

		MousePackets.queueClickPacket();
		WidgetPackets.queueWidgetOnWidget(item1.get(), item2.get());
		timeout = 2;
	}

	@Provides
	OneClickConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(OneClickConfig.class);
	}
}
