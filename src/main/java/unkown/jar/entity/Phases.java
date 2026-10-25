package unkown.jar.entity;

import net.minecraft.server.level.ServerLevel;
import unkown.jar.UnkownConfig;

/** Alle phasenabhaengigen Werte an einem Ort - hier kann man das Balancing anpassen. */
public final class Phases {
	public static final String[] NAMES = {"", "unknown", "unnowk", "ukwonoa", "HsiEpLdP", "neb"};

	/** Ingame-Tag, Tag 1 = erster Tag der Welt. Falls sich die Zeit-API aendert: nur diese Methode anpassen. */
	public static int day(ServerLevel level) {
		return (int) (level.getDayTime() / 24000L) + 1;
	}

	/** Tag 1 -> Phase 1, Tag 2 -> 2, Tag 3 -> 3, Tag 4 und danach -> 4. */
	public static int current(ServerLevel level) {
		int forced = UnkownConfig.get().forcePhase;
		if (forced >= 1 && forced <= 4) {
			return forced;
		}
		return Math.max(1, Math.min(4, day(level)));
	}

	private static int idx(int phase) {
		return Math.max(1, Math.min(5, phase));
	}

	public static String name(int phase) { return NAMES[idx(phase)]; }
	public static double scale(int phase) { return new double[]{0, 1.0, 1.2, 1.55, 2.3, 1.9}[idx(phase)]; }
	public static double speed(int phase) { return new double[]{0, 0.26, 0.31, 0.38, 0.44, 0.56}[idx(phase)]; }
	public static double damage(int phase) { return new double[]{0, 0, 3, 5, 7, 8}[idx(phase)]; }
	public static double health(int phase) { return new double[]{0, 40, 60, 100, 200, 150}[idx(phase)]; }
	public static int heightBlocks(int phase) { return (int) Math.ceil(1.95 * scale(phase)); }

	public static double minSpawnDist(int phase) { return new double[]{0, 28, 22, 30, 34, 32}[idx(phase)]; }
	public static double maxSpawnDist(int phase) { return new double[]{0, 42, 34, 42, 48, 46}[idx(phase)]; }

	/** Maximale Lebensdauer einer Begegnung in Ticks (20 Ticks = 1 Sekunde). */
	public static int minLife(int phase) { return new int[]{0, 400, 600, 1400, 1800, 1500}[idx(phase)]; }
	public static int maxLife(int phase) { return new int[]{0, 800, 1000, 1500, 1900, 1700}[idx(phase)]; }

	/** Pause bis zur naechsten Begegnung in Ticks. */
	public static int minGap(int phase) { return new int[]{0, 4800, 3600, 3600, 3000, 3000}[idx(phase)]; }
	public static int maxGap(int phase) { return new int[]{0, 10800, 8400, 7200, 6000, 6000}[idx(phase)]; }

	/** Lines the entity sometimes "says" in chat. */
	public static String[] chatLines(int phase) {
		return switch (idx(phase)) {
			case 1 -> new String[]{"...", "behind you", "i see you", "do not look away", "\u00a7kxxxxxx"};
			case 2 -> new String[]{"you saw me", "why are you looking", "i am closer than you think", "let us play", "ben was curious too"};
			case 3 -> new String[]{"run", "i will find you", "ben was slower", "you cannot leave", "i am not alone"};
			case 5 -> new String[]{"it hurts", "why did you not look for me", "i waited", "i was slower", "help me", "it is so dark here", "i am still ben", "mika", "jonas", "lena", "tim", "you can not outrun me anymore", "\u00a7kBenBenBen\u00a7r i am neb now", "the poster was me"};
			default -> new String[]{"IT IS TOO LATE", "i have you", "come back", "\u00a7kHsiEpLdP\u00a7r i see everything", "your game belongs to me"};
		};
	}

	private Phases() {
	}
}
