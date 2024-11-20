package io.github.haykam821.cornmaze.game.phase;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.haykam821.cornmaze.game.CornMazeConfig;
import io.github.haykam821.cornmaze.game.map.CornMazeMap;
import io.github.haykam821.cornmaze.game.map.CornMazeMapBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class CornMazeActivePhase {
	private static final DecimalFormat MINUTE_FORMAT = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final CornMazeMap map;
	private final CornMazeConfig config;
	private final Set<PlayerRef> players;
	private int ticks = 0;
	private int ticksUntilClose = -1;

	public CornMazeActivePhase(GameSpace gameSpace, ServerWorld world, CornMazeMap map, CornMazeConfig config, Set<PlayerRef> players) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.players = players;
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
		activity.deny(GameRuleType.UNSTABLE_TNT);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, CornMazeMap map, CornMazeConfig config) {
		Set<PlayerRef> players = gameSpace.getPlayers().stream().map(PlayerRef::of).collect(Collectors.toSet());
		CornMazeActivePhase phase = new CornMazeActivePhase(gameSpace, world, map, config, players);

		gameSpace.setActivity(activity -> {
			CornMazeActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.OFFER, phase::offerPlayer);
			activity.listen(GamePlayerEvents.REMOVE, phase::removePlayer);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
		});
	}

	private void enable() {
 		for (PlayerRef playerRef : this.players) {
			playerRef.ifOnline(this.world, player -> {
				player.changeGameMode(GameMode.ADVENTURE);
			});
		}

		for (BlockPos pos : this.map.getBarrierBounds()) {
			boolean ladder = this.config.getMapConfig().isSideways() && pos.getZ() == 1;
			BlockState state = ladder ? CornMazeMapBuilder.LADDER_STATE : CornMazeMapBuilder.AIR_STATE;

			this.world.setBlockState(pos, state);
		}
	}

	private void tick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
		}

		this.ticks += 1;

		for (PlayerRef playerRef : this.players) {
			playerRef.ifOnline(this.world, player -> {
				if (!this.map.getBox().contains(player.getPos())) {
					this.map.spawn(player, this.world);
				} else if (!this.isGameEnding() && this.map.getEndBox().contains(player.getPos())) {
					this.gameSpace.getPlayers().sendMessage(this.getWinMessage(player));
					this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
				}
			});
		}
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	private Text getWinMessage(ServerPlayerEntity winner) {
		return Text.translatable("text.cornmaze.win", winner.getDisplayName(), MINUTE_FORMAT.format(this.ticks / 20d / 60d)).formatted(Formatting.GOLD);
	}

	private PlayerOfferResult offerPlayer(PlayerOffer offer) {
		return this.map.acceptOffer(offer, this.world, GameMode.SPECTATOR);
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.players.remove(PlayerRef.of(player));
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.map.spawn(player, this.world);
		return ActionResult.FAIL;
	}

	static {
		MINUTE_FORMAT.setRoundingMode(RoundingMode.UP);
	}
}