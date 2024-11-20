package io.github.haykam821.cornmaze.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CornMazeMapConfig {
	public static final Codec<CornMazeMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(CornMazeMapConfig::getX),
			Codec.INT.fieldOf("z").forGetter(CornMazeMapConfig::getZ),
			Codec.INT.optionalFieldOf("height", 8).forGetter(CornMazeMapConfig::getHeight),
			Codec.INT.optionalFieldOf("x_scale", 4).forGetter(CornMazeMapConfig::getXScale),
			Codec.INT.optionalFieldOf("z_scale", 4).forGetter(CornMazeMapConfig::getZScale),
			Codec.BOOL.optionalFieldOf("sideways", false).forGetter(CornMazeMapConfig::isSideways),
			Codec.DOUBLE.optionalFieldOf("decay", 0d).forGetter(CornMazeMapConfig::getDecay)
		).apply(instance, CornMazeMapConfig::new);
	});

	private final int x;
	private final int z;
	private final int height;
	private final int xScale;
	private final int zScale;
	private final boolean sideways;
	private final double decay;

	public CornMazeMapConfig(int x, int z, int height, int xScale, int zScale, boolean sideways, double decay) {
		this.x = x % 2 == 0 ? x + 1 : x;
		this.z = z % 2 == 0 ? z + 1 : z;
		this.height = height;
		this.xScale = xScale;
		this.zScale = zScale;
		this.sideways = sideways;
		this.decay = decay;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}

	public int getHeight() {
		return this.height;
	}

	public int getXScale() {
		return this.xScale;
	}

	public int getZScale() {
		return this.zScale;
	}

	public boolean isSideways() {
		return this.sideways;
	}

	public double getDecay() {
		return this.decay;
	}
}