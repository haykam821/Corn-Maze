package io.github.haykam821.cornmaze;

import io.github.haykam821.cornmaze.game.CornMazeConfig;
import io.github.haykam821.cornmaze.game.phase.CornMazeWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "cornmaze";

	private static final Identifier CORN_MAZE_ID = Main.identifier("corn_maze");
	public static final GameType<CornMazeConfig> CORN_MAZE_TYPE = GameType.register(CORN_MAZE_ID, CornMazeConfig.CODEC, CornMazeWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}