# Blacklight Evolution - fix verification (ASCII-only, PowerShell-safe)
$root = "D:\Blacklight-Evolution-0.2.0-alpha.2\blacklight-evolution-neoforge"
$pass = 0; $fail = 0
function Check($name, $file, $pattern) {
    $full = Join-Path $root $file
    if (-not (Test-Path $full)) {
        Write-Host ("[SKIP] {0} (file missing: {1})" -f $name, $file) -ForegroundColor Yellow
        return
    }
    $content = Get-Content $full -Raw -Encoding UTF8
    if ($content -match $pattern) {
        Write-Host ("[PASS] {0}" -f $name) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("[FAIL] {0}  ({1})" -f $name, $file) -ForegroundColor Red
        $script:fail++
    }
}

Check "C2S enqueueWork" "src\main\java\com\codex\blacklight\network\ModNetwork.java" 'context\.enqueueWork'
Check "MovementInput isActive gate" "src\main\java\com\codex\blacklight\network\ModNetwork.java" 'PrototypeModeSystem\.isActive'
Check "canAct helper" "src\main\java\com\codex\blacklight\network\ModNetwork.java" 'canAct'
Check "acceptInput gate" "src\main\java\com\codex\blacklight\movement\PrototypeMovementSystem.java" 'PrototypeModeSystem\.isActive\(player\)'
Check "tick guard extended" "src\main\java\com\codex\blacklight\movement\PrototypeMovementSystem.java" 'isInWater'
Check "sprint flag restore" "src\main\java\com\codex\blacklight\movement\PrototypeMovementSystem.java" 'setSprinting\(movement\.sprinting'
Check "dash direction locked" "src\main\java\com\codex\blacklight\movement\MovementState.java" 'dashDirectionX'
Check "transform cooldown" "src\main\java\com\codex\blacklight\combat\ClawCombatState.java" 'canToggleTransform'
Check "groundspike grace" "src\main\java\com\codex\blacklight\combat\ClawCombatState.java" 'groundSpikeOffGroundTicks'
Check "dash sweep per tick" "src\main\java\com\codex\blacklight\combat\ClawCombatSystem.java" 'DASH_HITS'
Check "combo dir locked" "src\main\java\com\codex\blacklight\combat\ClawCombatSystem.java" 'combat\.dashX\(\), 0\.0D, combat\.dashZ\(\)'
Check "spike zero-vec guard" "src\main\java\com\codex\blacklight\combat\ClawCombatSystem.java" 'away\.lengthSqr\(\) < 1\.0E-5D'
Check "RightClickEmpty lock" "src\main\java\com\codex\blacklight\mode\PrototypeSurvivalLock.java" 'RightClickEmpty'
Check "notifyClient fixed" "src\main\java\com\codex\blacklight\mode\PrototypeSurvivalLock.java" 'isClientSide\(\)\)'
Check "infect excludes creative" "src\main\java\com\codex\blacklight\infection\BlacklightInfectionSystem.java" 'isCreative'
Check "scan throttle 20" "src\main\java\com\codex\blacklight\infection\BlacklightInfectionSystem.java" '% 20 != 0'
Check "pvp check" "src\main\java\com\codex\blacklight\infection\BlacklightInfectionSystem.java" 'isPvpAllowed'
Check "PostChain reload guard" "src\main\java\com\codex\blacklight\client\ClientSpeedPostEffect.java" 'lastResourceManager'
Check "PostChain logout" "src\main\java\com\codex\blacklight\client\ClientSpeedPostEffect.java" 'LoggingOut'
Check "ClawLayer lazy bake" "src\main\java\com\codex\blacklight\client\renderer\layer\BlacklightClawLayer.java" 'model == null'
Check "FOV impulse lerp" "src\main\java\com\codex\blacklight\client\ClientPhysicalFeedback.java" 'previousFovImpulse'
Check "LVS pruning" "src\main\java\com\codex\blacklight\client\PrototypeAnimationController.java" 'LocomotionVisualState\.remove\(player\.getId\(\)\)'
Check "LVS computeIfAbsent" "src\main\java\com\codex\blacklight\movement\LocomotionVisualState.java" 'computeIfAbsent'
Check "EF getPlayerFor null" "src\main\java\com\codex\blacklight\integration\epicfight\PrototypeComboActionAnimation.java" 'player == null \? 0\.0F'
Check "FMLEnvironment guard" "src\main\java\com\codex\blacklight\integration\epicfight\PrototypeComboActionAnimation.java" 'FMLEnvironment'
Check "InventoryScreen lock" "src\main\java\com\codex\blacklight\client\ClientClawEvents.java" 'InventoryScreen'
Check "EF version cap" "src\main\templates\META-INF\neoforge.mods.toml" '21\.17\.3\.1,21\.17\.4'
Check "pack.mcmeta" "src\main\resources\pack.mcmeta" 'pack_format'
Check "config-cache off" "gradle.properties" 'configuration-cache=false'
Check "Xmx4G" "gradle.properties" 'Xmx4G'
Check "loader [2,)" "gradle.properties" 'loader_version_range=\[2,\)'
Check "tmp gitignored" ".gitignore" 'tmp/'
Check "ClawTransform NaN guard" "src\main\java\com\codex\blacklight\client\tuning\ClawTransformSettings.java" 'Double\.isFinite'
Check "WallRun tests extended" "src\test\java\com\codex\blacklight\movement\WallRunPhysicsTest.java" 'Double\.NaN'
Check "GroundSpikeRules exact" "src\test\java\com\codex\blacklight\combat\GroundSpikeRulesTest.java" 'VISUAL_SPIKE_COUNT'
Check "user.dir test path" "src\test\java\com\codex\blacklight\movement\MovementTuningDefaultsTest.java" 'user\.dir'

# Negative checks
$cm = Get-Content (Join-Path $root "src\main\java\com\codex\blacklight\client\ClientMovementEvents.java") -Raw -Encoding UTF8
if ($cm -notmatch 'baseLayer\.animationPlayer') { Write-Host "[PASS] baseLayer direct access removed" -ForegroundColor Green; $pass++ } else { Write-Host "[FAIL] baseLayer direct access remains" -ForegroundColor Red; $fail++ }
$rel = Get-Content (Join-Path $root "src\main\java\com\codex\blacklight\combat\ClawCombatSystem.java") -Raw -Encoding UTF8
if ($rel -notmatch 'requestGroundSpikeRelease') { Write-Host "[PASS] dead release handler removed" -ForegroundColor Green; $pass++ } else { Write-Host "[FAIL] dead release handler remains" -ForegroundColor Red; $fail++ }
$crash = Get-ChildItem $root -Filter "hs_err*" -ErrorAction SilentlyContinue
if (-not $crash) { Write-Host "[PASS] crash logs deleted" -ForegroundColor Green; $pass++ } else { Write-Host "[FAIL] crash logs remain" -ForegroundColor Red; $fail++ }
if (-not (Test-Path (Join-Path $root "src\main\resources\assets\examplemod"))) { Write-Host "[PASS] examplemod removed" -ForegroundColor Green; $pass++ } else { Write-Host "[FAIL] examplemod remains" -ForegroundColor Red; $fail++ }

Write-Host ""
Write-Host ("=== RESULT: {0} PASS / {1} FAIL ===" -f $pass, $fail)
if ($fail -gt 0) { exit 1 } else { exit 0 }
