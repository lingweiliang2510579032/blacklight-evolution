# Combat targeting and approach

The server chooses an eligible, unobstructed target inside its loaded view range. An explicit lock has priority. Without a lock, each new attack selects the nearest visible entity in the forward view cone; distance wins over crosshair alignment. Releasing a charged press retains its chosen target. These are this mod's gameplay policies.

Acquisition range is separate from damage reach. Ordinary ground attacks acquire only within their reach plus the bounded 1.65-block local adjustment; locking a distant actor cannot trigger an automatic run-up. Ground specials and whip swings follow this same rule. Genuine aerial pursuit moves, including hammer toss, can physically approach a distant target using collision-aware velocity. The small hop of the grounded hammer combo finisher is excluded. The attack clock and weapon trails wait during aerial pursuit, retaining the move's anticipation pose. On arrival, the attack resumes its contact timeline. Damage still requires the local contact volume or whip curve to intersect the victim. Position changes during pursuit are tracked every server tick.

Losing the target, intervening solid blocks, a target teleport, stalled movement, or the bounded approach timeout cancels pursuit. It does not turn a missed attack into remote damage. Blade and hammer dives retain their landing branch; the authored claw pounce retains its contact branch. Charged and combo variants use the same approach contract.

Whip grabs distinguish ordinary victims from large victims: ordinary victims are reeled toward the hand, while a large victim initially pulls the player toward it. Inputs cannot override this hauling phase. The existing grab range remains separate from attack acquisition range.

Optional local `combat-motion-tuning.data` accompanies the private motion profile. It supplies per-action movement parameters; it is not bundled with public resources. A local source-field match establishes the serialized parameter, not a complete reconstruction of the original game's runtime target scoring. Minecraft distance conversion, visibility policy, timeouts and collision handling remain adaptations.

Regression source lives in `CombatTravelGameTest`, including the ground no-run-up policy. As requested by the user on 2026-09-22, subsequent changes are compiled and packaged only; automated tests and gameplay acceptance are not run without a new explicit request.
# Attack movement without a target

Flying kick and air cannonball have a finite propulsion phase even when selection finds no victim. The release heading determines travel, held charge scales speed and flight time, and the rendered striking pose remains active until contact, collision or timeout. Contact tests cover the body segment actually traversed; a victim entering the path can be struck without becoming a remote damage target. Recovery preserves some momentum and resumes normal falling.

Hammer toss uses a ballistic launch, with charge affecting its range; it is distinct from the downward elbow strike. Blade dive and hammer elbow retain horizontal motion and accelerate forward during descent. Blade rush uses its movement callback once, replacing clip root translation so displacement is not applied twice. Ground pound remains a downward area strike, and ordinary stationary attacks receive no extra dash.

Optional local tuning supplies serialized movement fields. Minecraft unit conversion, bounded camera pitch, collision handling and the user's view-wide pursuit rule are adaptations. Decoding serialized fields does not establish complete equivalence with the original game's movement engine.
