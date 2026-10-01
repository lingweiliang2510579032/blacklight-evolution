package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.BlacklightNeoMod;
import com.codex.blacklight.combat.LocalWeaponMotion;
import com.codex.blacklight.combat.WeaponMove;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LinkAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.api.utils.TimePairList;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/** Full-body local motion with its own same-source collision-aware displacement. */
public final class LocalWeaponAnimation extends ActionAnimation {
    private final WeaponMove move;
    private final LocalWeaponMotion.Definition definition;
    private volatile yesman.epicfight.api.animation.AnimationClip approachRun;

    public LocalWeaponAnimation(AnimationAccessor<? extends ActionAnimation> accessor, WeaponMove move) {
        super(LocalWeaponMotion.find(move.id).orElseThrow().transitionSeconds(), accessor, Armatures.BIPED);
        this.move = move;
        this.definition = LocalWeaponMotion.find(move.id).orElseThrow();
        addProperty(ActionAnimationProperty.MOVE_ON_LINK, false);
        addProperty(ActionAnimationProperty.MOVE_VERTICAL, false);
        addProperty(ActionAnimationProperty.STOP_MOVEMENT, !move.aerial());
        addProperty(ActionAnimationProperty.COORD_GET, (animation, patch, coord, previous, current) -> {
            float offset = releaseOffset(patch);
            return MoveCoordFunctions.MODEL_COORD.get(animation, patch, coord,
                    Math.max(offset, previous), Math.max(offset, current));
        });
    }

    private float releaseOffset(LivingEntityPatch<?> patch) {
        var visual = patch.getOriginal().getData(ModAttachments.WEAPON_VISUAL);
        return visual.spec() == move && !visual.charging() ? Math.max(0, visual.clipOffset()) : 0;
    }

    @Override protected boolean validateMovement(LivingEntityPatch<?> patch, AssetAccessor<? extends DynamicAnimation> animation) {
        // Air travel belongs to the server's collision-aware velocity, never a second Coord path.
        return !move.aerial()&&move!=WeaponMove.BLADE_RUSH&&!patch.getOriginal().getData(ModAttachments.WEAPON_VISUAL).traveling()&&super.validateMovement(patch,animation);
    }

    @Override public net.minecraft.world.phys.Vec3 getCoordVector(LivingEntityPatch<?> patch,AssetAccessor<? extends DynamicAnimation> animation) {
        var entity=patch.getOriginal();var visual=entity.getData(ModAttachments.WEAPON_VISUAL);
        // Rendered limbs, contact boxes and the native step must use the same yaw;
        // vanilla camera rotation is free to look elsewhere during an attack.
        if(visual.spec()==move&&patch instanceof yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch<?> playerPatch){
            playerPatch.setYRotO(visual.yaw());playerPatch.setModelYRot(visual.yaw(),false);
        }
        var step=super.getCoordVector(patch,animation);
        if(move.aerial()||move.rush()||move.quake())return step;
        var target=entity.level().getEntity(visual.targetId());
        if(target instanceof net.minecraft.world.entity.LivingEntity living&&living.isAlive()){
            var delta=living.position().subtract(entity.position()).multiply(1,0,1);double distance=delta.length();
            if(distance>.001){
                var toward=delta.scale(1/distance);double advance=step.dot(toward);
                double standOff=Math.max(.8,(entity.getBbWidth()+living.getBbWidth())*.5+.15);
                double available=Math.max(0,distance-standOff);
                if(advance>available)step=step.subtract(toward.scale(advance-available));
            }
        }
        return step;
    }

    @Override public void tick(LivingEntityPatch<?> patch) {
        var travel=patch.getOriginal().getData(ModAttachments.WEAPON_VISUAL);
        if(travel.spec()==move&&travel.attackPose()>=0){
            float pose=travel.attackPose();
            if(move==WeaponMove.AIR_PAIN){
                // Cannonball is a native cyclic roll during flight, unlike the
                // extended-leg flying kick which holds its striking posture.
                float phase=(patch.getOriginal().level().getGameTime()-travel.started())/20F;
                pose=move.start/20F+Math.max(0,phase%Math.max(.05F,(move.end-move.start)/20F));
            }
            patch.getAnimator().getPlayerFor(getAccessor()).setElapsedTime(pose,pose);
            super.tick(patch);return;
        }
        if(travel.spec()==move&&travel.traveling()){
            float time=Math.min(travel.travelPose(),Math.max(0,(patch.getOriginal().level().getGameTime()-travel.started())/20F));
            patch.getAnimator().getPlayerFor(getAccessor()).setElapsedTime(time,time);
            super.tick(patch);return;
        }
        if(move==WeaponMove.CLAW_POUNCE){
            var entity=patch.getOriginal();var visual=entity.getData(ModAttachments.WEAPON_VISUAL);
            if(visual.spec()==move&&!visual.charging()&&visual.clipOffset()<.5F){
                float time=Math.min(.40F,(entity.level().getGameTime()-visual.started())/20F-definition.transitionSeconds());
                time=Math.max(visual.clipOffset(),time);
                patch.getAnimator().getPlayerFor(getAccessor()).setElapsedTime(time,time);
            }
        }
        if(definition.landing().isPresent()){
            var entity=patch.getOriginal();var visual=entity.getData(ModAttachments.WEAPON_VISUAL);
            if(visual.spec()==move&&!visual.charging()){
                var landing=definition.landing().orElseThrow();
                float time=(entity.level().getGameTime()-visual.started())/20f-definition.transitionSeconds()
                        +visual.clipOffset()-Math.round(visual.clipOffset()*20)/20f;
                if(visual.clipOffset()<landing.impactTime()-.001f)time=Math.min(time,landing.holdTime());
                time=Math.clamp(time,visual.clipOffset(),getTotalTime()-.0001f);
                patch.getAnimator().getPlayerFor(getAccessor()).setElapsedTime(time,time);
            }
        }
        super.tick(patch);
    }

    @Override public float getPlaySpeed(LivingEntityPatch<?> patch,DynamicAnimation animation){
        var visual=patch.getOriginal().getData(ModAttachments.WEAPON_VISUAL);
        return visual.spec()==move&&(visual.traveling()||visual.attackPose()>=0||definition.landing().isPresent())
                &&!animation.isLinkAnimation()?0:super.getPlaySpeed(patch,animation);
    }

    @Override public Pose getPoseByTime(LivingEntityPatch<?> patch,float time,float partial){
        var visual=patch.getOriginal().getData(ModAttachments.WEAPON_VISUAL);
        if(visual.spec()==move&&visual.traveling()&&!move.aerial()&&approachRun!=null){
            float clock=(patch.getOriginal().level().getGameTime()+partial-visual.started())/20F;
            return approachRun.getPoseInTime(Math.max(0,clock%approachRun.getClipTime()));
        }
        return super.getPoseByTime(patch,time,partial);
    }

    @Override public void setLinkAnimation(AssetAccessor<? extends DynamicAnimation> fromAnimation, Pose startPose,
                                          boolean sameLayer, float transitionModifier,
                                          LivingEntityPatch<?> patch, LinkAnimation dest) {
        float offset = releaseOffset(patch);
        // Epic Fight uses a NEGATIVE transition modifier to seek the destination clip.
        super.setLinkAnimation(fromAnimation, startPose, sameLayer,
                offset > 0 ? -offset : transitionModifier, patch, dest);
    }

    @Override public void loadAnimation() {
        var loader = new JsonAssetLoader(definition.animation().deepCopy(),
                ResourceLocation.fromNamespaceAndPath("blacklight", "local/" + move.id));
        animationClip = loader.loadClipForAnimation(this);
        if (getProperty(ActionAnimationProperty.COORD).isEmpty())
            throw new IllegalStateException("Local motion has no movement track: " + move.id);
        animationClip.bakeKeyframes();
        var run=com.codex.blacklight.combat.LocalVisualMotion.find("basic_sprint");
        if(run.isPresent()){
            var originalCoord=getProperty(ActionAnimationProperty.COORD).orElseThrow();
            var clip=new JsonAssetLoader(run.orElseThrow().animation().deepCopy(),ResourceLocation.fromNamespaceAndPath("blacklight","local/basic_sprint")).loadClipForAnimation(this);
            clip.bakeKeyframes();approachRun=clip;
            addProperty(ActionAnimationProperty.COORD,originalCoord);
        }
        addProperty(ActionAnimationProperty.MOVE_TIME, TimePairList.create(0, animationClip.getClipTime()));
        BlacklightNeoMod.LOGGER.info("Loaded local full-body weapon motion {}: {} seconds, contact ticks {}..{}",
                move.id, animationClip.getClipTime(), move.start, move.end);
    }
}
