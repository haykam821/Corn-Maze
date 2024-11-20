package io.github.haykam821.cornmaze.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class CornMazeMap {
	private final MapTemplate template;
	private final Box box;
	private final Box startBox;
	private final Box endBox;
	private final BlockBounds barrierBounds;

	private final Vec3d spawn;

	private final float spawnYaw;
	private final float spawnPitch;

	public CornMazeMap(MapTemplate template, BlockBounds bounds, BlockBounds startBounds, BlockBounds endBounds, BlockBounds barrierBounds, Vec3d spawn, float spawnYaw, float spawnPitch) {
		this.template = template;
		this.box = bounds.asBox();
		this.startBox = startBounds.asBox();
		this.endBox = endBounds.asBox();
		this.barrierBounds = barrierBounds;

		this.spawn = spawn;

		this.spawnYaw = spawnYaw;
		this.spawnPitch = spawnPitch;
	}

	public Box getBox() {
		return this.box;
	}

	public Box getStartBox() {
		return this.startBox;
	}

	public Box getEndBox() {
		return this.endBox;
	}

	public BlockBounds getBarrierBounds() {
		return this.barrierBounds;
	}

	public void spawn(ServerPlayerEntity player, ServerWorld world) {
		player.teleport(world, this.spawn.getX(), this.spawn.getY(), this.spawn.getZ(), this.spawnYaw, this.spawnPitch);
	}

	public PlayerOfferResult.Accept acceptOffer(PlayerOffer offer, ServerWorld world, GameMode gameMode) {
		return offer.accept(world, this.spawn).and(() -> {
			offer.player().changeGameMode(gameMode);

			offer.player().setYaw(this.spawnYaw);
			offer.player().setPitch(this.spawnPitch);
		});
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}