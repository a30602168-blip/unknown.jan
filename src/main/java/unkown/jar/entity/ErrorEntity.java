package unkown.jar.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import unkown.jar.UnkownConfig;
import unkown.jar.director.SpawnFinder;

import java.util.UUID;

/**
 * ERROR, the script monster. A completely separate entity (no phases): tall, fast, glitching.
 * It appears and blinks around you ("script paths"), shows a 5 second countdown, then chases you,
 * teleports when you get far away, and slashes with its long claws. It regenerates and writes
 * error messages into the chat. Own model, own sounds.
 */
public class ErrorEntity extends PathfinderMob {
	public static final String NAME = "ERROR";
	private static final int ATTACK_DURATION = 14;   // ticks of the swing animation
	private static final int COUNTDOWN_TICKS = 100;  // 5 seconds head start
	private static final int CHASE_MAX_TICKS = 1100; // about 55 seconds

	private static final EntityDataAccessor<Integer> DATA_ATTACK =
			SynchedEntityData.defineId(ErrorEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_CHASING =
			SynchedEntityData.defineId(ErrorEntity.class, EntityDataSerializers.BOOLEAN);

	private enum Mode { APPEAR, COUNTDOWN, CHASE }

	private Mode mode = Mode.APPEAR;
	private UUID targetId;
	private boolean initialized;
	private int modeTicks;
	private int life;
	private int attackCooldown;
	private int teleportCooldown;
	private int soundTimer = 40;
	private int chatLines;
	private int hits;
	private int lostSightTicks;
	private boolean hitDone;

	public ErrorEntity(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 120.0)
				.add(Attributes.MOVEMENT_SPEED, 0.50)
				.add(Attributes.FOLLOW_RANGE, 90.0)
				.add(Attributes.ATTACK_DAMAGE, 6.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.SCALE, 1.45);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ATTACK, 0);
		builder.define(DATA_CHASING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
	}

	public int getAttackTicks() {
		return this.entityData.get(DATA_ATTACK);
	}

	public boolean isChasing() {
		return this.entityData.get(DATA_CHASING);
	}

	public void bindToPlayer(UUID playerId) {
		this.targetId = playerId;
	}

	// ------------------------------------------------------------------ tick

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel sl) {
			serverTick(sl);
		}
	}

	private void serverTick(ServerLevel sl) {
		if (!this.initialized) {
			this.initialized = true;
			setCustomName(Component.literal(NAME));
			setCustomNameVisible(false);
		}
		this.life++;
		this.modeTicks++;
		if (this.attackCooldown > 0) {
			this.attackCooldown--;
		}
		if (this.teleportCooldown > 0) {
			this.teleportCooldown--;
		}
		if (this.life % 20 == 0 && getHealth() < getMaxHealth()) {
			heal(1.0F); // it regenerates
		}
		int attack = getAttackTicks();
		if (attack > 0) {
			this.entityData.set(DATA_ATTACK, attack - 1);
		}

		ServerPlayer target = resolveTarget(sl);
		if (target == null) {
			discard();
			return;
		}
		double dist = distanceTo(target);
		boolean peaceful = sl.getDifficulty() == Difficulty.PEACEFUL;

		switch (this.mode) {
			case APPEAR -> appear(sl, target);
			case COUNTDOWN -> countdown(sl, target);
			case CHASE -> chase(sl, target, dist, peaceful);
		}
		if (!isRemoved()) {
			ambient(sl, target);
		}
	}

	private ServerPlayer resolveTarget(ServerLevel sl) {
		Player p = this.targetId == null ? sl.getNearestPlayer(this, 128.0) : sl.getPlayerByUUID(this.targetId);
		if (p instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator()) {
			this.targetId = sp.getUUID();
			return sp;
		}
		return null;
	}

	// ------------------------------------------------------------------ modes

	/** It stands in the distance and blinks around you through "script paths". */
	private void appear(ServerLevel sl, ServerPlayer target) {
		getNavigation().stop();
		getLookControl().setLookAt(target, 60.0F, 60.0F);
		if (this.modeTicks == 1) {
			Fx.play(sl, ModSounds.ERROR_STATIC, getX(), getY(), getZ(), 2.5F, 1.0F);
			say(target, "script not found");
		}
		if (this.modeTicks % 40 == 0 && this.modeTicks < 160) {
			blink(sl, target, 16.0, 26.0);
		}
		if (this.modeTicks > 160) {
			this.mode = Mode.COUNTDOWN;
			this.modeTicks = 0;
		}
	}

	private void countdown(ServerLevel sl, ServerPlayer target) {
		getNavigation().stop();
		getLookControl().setLookAt(target, 60.0F, 60.0F);
		if (this.modeTicks == 1) {
			target.connection.send(new ClientboundSetTitlesAnimationPacket(0, 24, 4));
		}
		if (this.modeTicks % 20 == 1 && this.modeTicks <= COUNTDOWN_TICKS) {
			int seconds = 5 - (this.modeTicks - 1) / 20;
			target.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("\u00a77\u00a7oERROR: player not found")));
			target.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00a7c\u00a7l" + seconds)));
			Fx.play(sl, ModSounds.ERROR_BEEP, target.getX(), target.getY(), target.getZ(), 1.5F, 0.8F + 0.1F * (5 - seconds));
		}
		if (this.modeTicks > COUNTDOWN_TICKS) {
			target.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 8));
			target.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
			target.connection.send(new ClientboundSetTitleTextPacket(Component.literal("\u00a74\u00a7lERROR")));
			this.mode = Mode.CHASE;
			this.modeTicks = 0;
			this.hits = 0;
			this.lostSightTicks = 0;
			this.entityData.set(DATA_CHASING, true);
			Fx.play(sl, ModSounds.ERROR_SCREAM, getX(), getY(), getZ(), 3.0F, 1.0F);
			say(target, "while(true) { error(); }");
		}
	}

	private void chase(ServerLevel sl, ServerPlayer target, double dist, boolean peaceful) {
		getLookControl().setLookAt(target, 60.0F, 60.0F);
		if (this.modeTicks % 4 == 0) {
			getNavigation().moveTo(target, 1.0);
		}
		if (getNavigation().isDone() && this.modeTicks % 20 == 0) {
			getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
		}
		this.lostSightTicks = hasLineOfSight(target) ? 0 : this.lostSightTicks + 1;

		// "script path": when you get far away it blinks close to you
		if (dist > 22.0 && this.teleportCooldown == 0) {
			blink(sl, target, 10.0, 14.0);
			this.teleportCooldown = 160;
		}

		// slash attack with a long swing animation; the hit lands in the middle of the swing
		double reach = getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 1.8;
		if (getAttackTicks() == 0 && this.attackCooldown == 0 && !peaceful
				&& distanceToSqr(target) < reach * reach && Math.abs(target.getY() - getY()) < getBbHeight()) {
			this.entityData.set(DATA_ATTACK, ATTACK_DURATION);
			this.attackCooldown = 34;
			this.hitDone = false;
			Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 1.4F, 0.8F);
		}
		if (getAttackTicks() > 0 && getAttackTicks() <= ATTACK_DURATION / 2 && !this.hitDone) {
			this.hitDone = true;
			double r2 = reach + 0.8;
			if (distanceToSqr(target) < r2 * r2) {
				float dmg = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * UnkownConfig.get().damageMultiplier);
				if (target.hurtServer(sl, damageSources().mobAttack(this), dmg)) {
					this.hits++;
					Fx.play(sl, ModSounds.ERROR_HIT, target.getX(), target.getY(), target.getZ(), 1.6F, 0.9F);
					Fx.particles(sl, ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 14, 0.5);
				}
			}
		}

		boolean lost = dist > 85.0 || this.lostSightTicks > 240;
		if (this.modeTicks > CHASE_MAX_TICKS || lost || this.hits >= 5) {
			Fx.play(sl, ModSounds.ERROR_SCREAM, getX(), getY(), getZ(), 2.0F, 0.7F);
			say(target, "return null;");
			vanish(sl);
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Glitch teleport to a spot at the given distance from the player. */
	private void blink(ServerLevel sl, ServerPlayer target, double min, double max) {
		for (int i = 0; i < 12; i++) {
			double angle = this.random.nextDouble() * Math.PI * 2;
			double d = min + this.random.nextDouble() * (max - min);
			BlockPos spot = SpawnFinder.groundAt(sl, target.getX() + Math.cos(angle) * d, target.getZ() + Math.sin(angle) * d, target.getBlockY(), 3);
			if (spot != null) {
				Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 1.5F, 1.0F);
				Fx.particles(sl, ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() / 2, getZ(), 25, 0.6 * getScale());
				getNavigation().stop();
				setPos(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
				Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 1.8F, 0.7F);
				Fx.particles(sl, ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() / 2, getZ(), 25, 0.6 * getScale());
				return;
			}
		}
	}

	private void ambient(ServerLevel sl, ServerPlayer target) {
		if (--this.soundTimer > 0) {
			return;
		}
		Fx.play(sl, ModSounds.ERROR_STATIC, getX(), getY(), getZ(), 2.0F, 0.9F + this.random.nextFloat() * 0.3F);
		this.soundTimer = 100 + this.random.nextInt(140);
		if (this.random.nextFloat() < 0.4F) {
			say(target, null);
		}
	}

	private static final String[] LINES = {
			"error", "script not found", "line 42 corrupted", "while(true) { error(); }", "return null;",
			"invalid syntax", "REJECTED", "player.exe has stopped responding", "segmentation fault",
			"it does not exist. and yet it is here.", "i am not a monster. i am an error.", "fix me. you can not."
	};

	private void say(ServerPlayer target, String text) {
		if (!UnkownConfig.get().chatMessages || this.chatLines >= 5) {
			return;
		}
		this.chatLines++;
		String line = text != null ? text : LINES[this.random.nextInt(LINES.length)];
		target.sendSystemMessage(Component.literal("\u00a7c<" + NAME + "> \u00a77" + line));
	}

	public void vanish(ServerLevel sl) {
		Fx.play(sl, ModSounds.ERROR_GLITCH, getX(), getY(), getZ(), 2.0F, 0.6F);
		Fx.particles(sl, ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() / 2, getZ(), 50, 0.8 * getScale());
		Fx.particles(sl, ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 30, 0.8 * getScale());
		discard();
	}

	// ------------------------------------------------------------------ damage & sounds

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return super.hurtServer(level, source, amount);
		}
		if (getHealth() - amount <= getMaxHealth() * 0.2F) {
			vanish(level); // it glitches away instead of dying
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ERROR_GLITCH;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.ERROR_SCREAM;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(ModSounds.ERROR_HIT, 0.5F, 1.4F + this.random.nextFloat() * 0.3F);
	}
}
