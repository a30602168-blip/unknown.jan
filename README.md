# unknown.jar

Fabric horror mod for **Minecraft 1.21.11** (Fabric Loader >= 0.20.1, Fabric API, Java 21).

An entity follows you across several in-game days and gets more dangerous every day.

## Install
1. Install Fabric Loader for 1.21.11 and put Fabric API for 1.21.11 into your `mods` folder.
2. Build with `./gradlew build` and copy `build/libs/unkownjar-1.0.0.jar` (not the `-sources.jar`) into `mods`.
3. The mod must be installed on both the client and the server.

## Phases (by in-game day)
| Day | Phase | Name | Behaviour |
|---|---|---|---|
| 1 | 1 | unknown | pitch black, watches from afar, creeps closer only when you look away, vanishes on eye contact, never attacks |
| 2 | 2 | unnowk | pale and visible, bigger, teleports behind you, sometimes attacks (5 s countdown first) |
| 3 | 3 | ukwonoa | much bigger, hunched, glowing white eyes, 5 s countdown, then an active hunt |
| 4+ | 4 | HsiEpLdP | huge, black body with a bone-white face and huge toothy maw, no human legs: instead eight long, thin, angular spider legs that skitter around it; disturbing sounds, long hunts; if it catches you, you are kicked to the main menu |

## neb (Ben in monster form)
neb is the friend who was taken, read his name backwards (`unkownjar:neb`, test with `/summon unkownjar:neb`). In phase 4 about every fourth encounter is neb instead of HsiEpLdP.
He is black with red, blue and teal blocks of his old clothes left on his body, his head is almost black with two tiny pale eyes, two human legs hang wrong, and six thick, arching legs rise over his back and hammer the ground. He is **faster than a sprinting player** and talks a lot in the chat (`<neb> i waited`, `<neb> mika`, `<neb> i am still ben` ...). The usual 5 second countdown, time-limited hunts and the kick/jumpscare/crash rules apply to him too.

## ERROR (script monster, separate entity)
A completely different entity from the phases (`unkownjar:error`, test with `/summon unkownjar:error`): a very tall, thin black figure with a monitor for a head (one big eye, a huge toothy grin), hanging cables, extremely long arms with four-fingered claws, long legs with claw feet, and floating code windows (`error / script not found / line 42 corrupted`, `while(true) { error(); }`, `invalid syntax ... REJECTED`, `return null;`) and glitch shards orbiting it.
- **Behaviour:** it appears and "blinks" around you through script paths, shows the 5 second countdown (red numbers), then chases you at high speed, teleports close to you when you get far away, and slashes with its claws (own attack animation). It regenerates and glitches away instead of dying. Hunts are time limited (about 55 s).
- **Animations:** own idle twitching, walk, sprint (long strides, body thrown forward, arms swept back, cables streaming behind) and a two-part attack swing (claws up and back, then a heavy slash).
- **Sounds:** its own set: static, glitch zaps, a digital scream, error beeps and a crunching hit.
- **Chat:** writes things like `<ERROR> line 42 corrupted`.
- From phase 2 on, about 15% of encounters are ERROR (`errorChance`, `errorEnabled` in the config).

## Countdown, chat and events
- **5 second countdown:** before phase 2 attacks or phase 3/4 starts a hunt, the entity stands still and a countdown (5 ... 1, then "RUN!") appears in the middle of your screen. Run. Afterwards it attacks / hunts you.
- **Chat:** the entity sometimes writes lines like `<unnowk> you saw me` into the chat (max 1-2 per encounter).
- **Join event:** at a random moment (after about 3 minutes of play time) the chat shows `unknown joined the game`. This happens only once per player and world (stored in `unkownjar_events.txt` in the world folder; delete it to allow it again).
- **Water:** over water the entity conjures blackstone under its feet so it can walk across; the water returns when it moves on or vanishes.

## Death
- If an unknown entity kills you, a full-screen **jumpscare** appears with a very loud, distorted sound (plays on the master volume, not the monster slider). **Warning: loud and flashing.**
- About 4 seconds later your Minecraft **crashes** (singleplayer worlds are saved first, no files are deleted).
- Both can be switched off in the config.

## Abandoned buildings with lore
Rarely in the overworld you find abandoned buildings (`unkownjar:abandoned_building`). There are five different looks:
a spruce cabin, an oak cottage, a mossy stone longhouse, a dark oak/blackstone shack and a tent camp.
Each has a chest with 2-3 of 7 tattered books (Rules, The Night, The Morning, School, The Poster, unknown, neb) telling the story of five friends who played without cheats, and what happened to Ben.
Test with `/locate structure unkownjar:abandoned_building`.

## Fairness
- At most one entity per player, with long random pauses (about 3-10 minutes).
- Phases 3/4 always warn you with a 5 second countdown. Hunts are time limited, the entity loses you after about 12 s without line of sight and cannot open doors or break blocks.
- Phase 4 only kicks players it actually catches.

## Config: `config/unkownjar.json`
`forcePhase` (0 = automatic, 1-4 = force), `encounterIntervalMultiplier` (0.05 = very often), `firstEncounterDelaySeconds`,
`disconnectEnabled`, `disconnectChance`, `creativePlayersImmune`, `crashOnDeath`, `jumpscare`, `chatMessages`, `joinEvent`,
`errorEnabled`, `errorChance`, `damageMultiplier`, `huntSpeedMultiplier`, `phantomSounds`, `enabled`.

## Testing
- Set `forcePhase` and `encounterIntervalMultiplier: 0.05` in the config, or use `/time add 24000` and `/summon unkownjar:unknown`.
- Play in survival mode: creative players are immune.
