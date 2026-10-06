package org.starloco.locos.dynamic

import org.starloco.locos.area.map.GameMap
import org.starloco.locos.client.Player
import org.starloco.locos.entity.npc.Npc
import java.util.HashMap

class Start(player: Player?) {

    private var player: Player? = null
    private var helper: Npc? = null
    @JvmField
    var leave = false
    private var map: GameMap? = null
    private val mapUse = HashMap<Int, GameMap>()
    private var thread: Thread? = null

    init {
        this.player = player
        this.player!!.start = this
    }

//	private class verifyIsOnline implements Runnable {
//		@SuppressWarnings("deprecation")
//		@Override
//		public void run() {
//			while(Start.this.player.isOnline)
//				try { Thread.sleep(250); } catch (Exception ignored) {}
//
//			player = null;
//			helper = null;
//			map = null;
//			mapUse.clear();
//			thread.interrupt();
//			thread.stop();
//		}
//	}
//
//	private class starting implements Runnable {
//		@SuppressWarnings("deprecation")
//		@Override
//		public void run() {
//			/* START : Construction de l'nvironement **/
//			try {
//			mapUse.put(1, World.world.getMap(6824).getMapCopy());
//			mapUse.put(2, World.world.getMap(6826).getMapCopy());
//			mapUse.put(3, World.world.getMap(6828).getMapCopy());
//
//
//			MapData data = mapUse[1].data;
//			if(data instanceof SQLMapData) {
//				((SQLMapData) data).addOnCellStopAction(329, 999, "192", "-1");
//			}
//
//			data = mapUse[1].data;
//			if(data instanceof SQLMapData) {
//				((SQLMapData) data).addOnCellStopAction(325, 999, "224", "-1");
//			}
//
//			data = mapUse[2].data;
//			if(data instanceof SQLMapData) {
//				((SQLMapData) data).addOnCellStopAction(192, 999, "389", "-1");
//			}
//
//			/* MAP 1 : Talk & Walk to begin Fight **/
//			try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
//			map = mapUse[1];
//			helper = map.addNpc(15020, (short) 179, 3);
//			// FIXME player.setSpellsPlace(false);
//			Start.this.player.unlearnSpell(661);
//			Start.this.player.teleport(map, 224);
//			Start.this.player.setBlockMovement(true);
//			SocketManager.GAME_SEND_ADD_NPC(Start.this.player, Start.this.helper);
//			try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Bienvenue "+Start.this.player.getName()+", je suis le gardien d'Amakna.");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Je vais te proposer de faire un choix...");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Si tu désires que l'on t'aide à faire tes premiers pas dans ce monde...");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "... rejoins mon ami dans la salle suivante, il doit t'attendre...");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "... pour cela, marche sur ce plot de transfert.");
//			Start.this.player.send("Gf-1|329");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Autrement, si tu désires découvrir le monde par toi même, marche sur l'autre plot.");
//			Start.this.player.send("Gf-1|325");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "... rejoins mon ami dans la salle suivante, il doit t'attendre...");
//			Start.this.player.setBlockMovement(false);
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//
//			int trying = 0;
//			while(Start.this.player.getCurMap().id == 6824 && !leave) {
//				try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
//				if(trying % 10 == 1)
//					SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Il faut que tu marches sur l'un des deux plots de transfert pour passer à la suite.");
//				trying++;
//			}
//
//			if(leave) {
//				SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Si tu es certain de ne vouloir aucune aide, clique une nouvelle fois sur le plot.");
//				try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//				SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Bonne chance !");
//				player = null;
//				helper = null;
//				map = null;
//				mapUse.clear();
//				thread.interrupt();
//				thread.stop();
//				return;
//			}
//
//			map.RemoveNpc(helper.id);
//	        map = mapUse[2];
//	        helper = map.addNpc(50000, (short) 210, 3);
//	        SocketManager.GAME_SEND_ADD_NPC_TO_MAP(Start.this.map, Start.this.helper);
//
//			trying = 0;
//			while(Start.this.player.getCurMap().id == 6824) {
//				try { Thread.sleep(250); } catch (InterruptedException ignored) {}
//				if(trying % 80 == 1)
//					SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Gardien Amakna", "Il faut que tu marches sur l'un des deux plots de transfert pour passer à la suite.");
//				trying++;
//			}
//
//			SocketManager.GAME_SEND_ADD_NPC_TO_MAP(Start.this.map, Start.this.helper);
//	        Start.this.player.setBlockMovement(true);
//
//	        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//	        SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Approche dans la lumière que je vois quel guerrier tu es !");
//			try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
//
//			String pathstr;
//			try {
//				pathstr = PathFinding.getShortestStringPathBetween(map, player.getCurCell().id, 238, 0);
//			} catch(Exception e) {return;}
//			if(pathstr == null)
//				return;
//			SocketManager.GAME_SEND_GA_PACKET(player.getGameClient(), "0", "1", player.id+"", pathstr);
//
//			player.getCurCell().removePlayer(player);
//			player.setCurCell(map.getCase(238));
//			map.addPlayer(player);
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			player.orientation = 7;
//			SocketManager.GAME_SEND_eD_PACKET_TO_MAP(map, player.id, 7);
//
//			try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Bien, voyons maintenant comment lancer un sort.");
//			try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Pour t'exercer, je te prête mon sort d'entrainement.");
//			try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
//			Start.this.player.learnSpell(661, 1, 'b');
//			player.setBlockMovement(false);
//			try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
//
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Prenons notre ami l'épouvantail ...");
//			try { Thread.sleep(500); } catch (InterruptedException ignored) {}
//			map.spawnGroupOnCommand(224, "1003,1,1;", false);
//			SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(map, player);
//
//
//			try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "... Attaque l'épouvantail !");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//
//			boolean say = false;
//			while(map.getMobGroups().size() > 0) {
//				if(!say)
//					SocketManager.GAME_SEND_cMK_PACKET_TO_MAP(map, "", -1, "Ganymede", "dynamic.start.poutch");
//				try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
//				say = true;
//			}
//
//			say = true;
//			while(player.fight != null && say) {
//				if(player.fight.isBegin())
//					say = false;
//				try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
//			}
//
//			try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "#", helper.id, "Ganymede", "Voyons ce que donne ce petit sort ...");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "#", helper.id, "Ganymede", "Pour cela clique sur le sort que je t'ai donné.");
//
//			while(player.fight != null)
//				try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
//
//			player.setBlockMovement(true);
//
//			try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Tu es maintenant prêt à faire ton premier combat.");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			Start.this.player.unlearnSpell(661);
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Je vais donc reprendre mon sort d'entrainement.");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//
//			data = mapUse[2].data;
//			if(data instanceof SQLMapData) {
//				((SQLMapData) data).addOnCellStopAction(177, 999, "388", "-1");
//			}
//
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Suis-moi dans la prochaine salle, tu auras 3 nouveaux sorts.");
//			try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
//			player.setBlockMovement(false);
//
//			try {
//				pathstr = PathFinding.getShortestStringPathBetween(map, helper.getCellId(), 177, 0);
//			} catch(Exception e) { return; }
//
//			if(pathstr == null) return;
//			SocketManager.GAME_SEND_GA_PACKET(player.getGameClient(), "0", "1", helper.id+"", pathstr);
//			try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
//
//			map.RemoveNpc(helper.id);
//			SocketManager.GAME_SEND_ERASE_ON_MAP_TO_MAP(map, helper.id);
//
//			map = mapUse[3];
//			map.addNpc(50001, (short) 299, 1);
//			map.spawnGroupOnCommand(311, "432,1,1;", false);
//
//			while(!map.getPlayers().contains(player))
//				try { Thread.sleep(250); } catch (InterruptedException ignored) {}
//
//			SocketManager.GAME_SEND_MAP_MOBS_GMS_PACKETS_TO_MAP(map, player);
//			player.setBlockMovement(true);
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Bien, tu sais désormais comment te battre contre un ennemi.");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			// FIXME player.setSpellsPlace(true);
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Je viens de te donner tes trois premiers sorts.");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Utilise les pour combattre l'Arakne qui se trouve dans cette pièce.");
//			try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
//			SocketManager.GAME_SEND_cMK_PACKET(player, "", helper.id, "Ganymede", "Si tu arrives à la vaincre, tu gagneras un niveau. Reviens me voir dès que tu seras niveau 2.");
//			player.setBlockMovement(false);
//			thread.interrupt();
//			} catch(Exception ignored) {}
//		}
//	}

}
