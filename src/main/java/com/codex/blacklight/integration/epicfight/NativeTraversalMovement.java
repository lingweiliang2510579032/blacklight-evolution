package com.codex.blacklight.integration.epicfight;

import com.codex.blacklight.movement.*;
import com.codex.blacklight.registry.ModAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.*;
import yesman.epicfight.api.asset.JsonAssetLoader;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import java.util.*;

/** Locally installed native poses; the existing movement system remains the sole physics owner. */
public final class NativeTraversalMovement extends MovementAnimation {
    public enum Family {
        IDLE("basic_idle",true),WALK("basic_walk",true),RUN("basic_sprint",true),
        RISE("basic_jump_rise",false),FALL("basic_fall",true),
        WALL_RUN("basic_wall_run_up",true),WALL_CLIMB("basic_wall_climb_up",true),
        WALL_CLING("basic_wall_cling",true),WALL_LEAP("basic_wall_leap",false),
        WALL_MANTLE("basic_wall_mantle",false),WALL_VAULT("basic_wall_vault",false),
        GLIDE("basic_glide_forward",true);
        final String id;final boolean loop;
        Family(String id,boolean loop){this.id=id;this.loop=loop;}
    }
    private final Family family;
    // Animation instances are shared by the integrated server and render thread.
    // Publish only fully baked clips, including a simultaneous first wall transition.
    private final Map<String,AnimationClip> clips=new java.util.concurrent.ConcurrentHashMap<>();
    // Entity.equals compares numeric IDs; client/server counterparts share an ID
    // but must never share a clock. Guava weak keys use object identity.
    private final java.util.concurrent.ConcurrentMap<LivingEntity,Clock> clocks=
            new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static final class Clock {String key;double clock,time,changed;Pose last,from;}
    public NativeTraversalMovement(AnimationAccessor<? extends MovementAnimation> accessor,Family family){
        super(.12F,family.loop,accessor,Armatures.BIPED);this.family=family;
    }
    private AnimationClip clip(String key){
        return clips.computeIfAbsent(key,id->{
            var data=NativeTraversalLibrary.require(id);
            var result=new JsonAssetLoader(data.animation().deepCopy(),ResourceLocation.fromNamespaceAndPath("blacklight","local/"+id)).loadClipForAnimation(this);
            result.bakeKeyframes();return result;
        });
    }
    @Override public void loadAnimation(){animationClip=clip(family.id);}
    @Override public float getPlaySpeed(LivingEntityPatch<?> patch,DynamicAnimation animation){return 1;}
    @Override public Pose getPoseByTime(LivingEntityPatch<?> patch,float time,float partial){
        var entity=patch.getOriginal();var velocity=entity.getDeltaMovement();double speed=velocity.horizontalDistance();
        double yaw=Math.toRadians(entity.yBodyRot);
        var wall=WallPoseState.get(entity);
        double right=wall==null?TraversalMotionSelector.rightward(velocity.x,velocity.z,entity.yBodyRot)
                :velocity.x*wall.normalZ()-velocity.z*wall.normalX();
        double forward=-velocity.x*Math.sin(yaw)+velocity.z*Math.cos(yaw);
        var snapshot=LocomotionVisualState.get(entity.getId());var phase=snapshot.phase();
        Clock state=clocks.computeIfAbsent(entity,k->new Clock());
        double now=(entity.level().getGameTime()+partial)/20.0;
        String key=switch(family){
            case IDLE -> switch(entity.getData(ModAttachments.PLAYER_STATE).getActivePower()){
                case CLAWS -> "basic_idle_claw";case BLADE -> "basic_idle_blade";
                case HAMMERFISTS -> "basic_idle_hammer";case WHIPFIST -> "basic_idle_whip";default -> family.id;
            };
            case WALK,RUN -> TraversalMotionSelector.ground(phase,family==Family.RUN||snapshot.sprint(),speed);
            case WALL_RUN,WALL_CLIMB -> TraversalMotionSelector.wall(family==Family.WALL_RUN,velocity.y,right);
            case RISE -> state.key!=null&&now-state.clock<.2?state.key:speed>.3?"basic_jump_running":velocity.y<.8?"basic_jump_tactical":family.id;
            case GLIDE -> TraversalMotionSelector.glide(forward,right);
            default -> family.id;
        };
        AnimationClip selected=clip(key);boolean fresh=state.key==null||now-state.clock>.2||now<state.clock;
        if(fresh||!key.equals(state.key)){
            boolean preserve=!fresh&&loop(state.key)&&loop(key)&&(family==Family.WALK||family==Family.RUN);
            double phaseTime=preserve?state.time/Math.max(.001,clip(state.key).getClipTime()):0;
            state.from=fresh?null:state.last;state.changed=now;state.time=(phaseTime%1)*selected.getClipTime();state.key=key;
        }
        double nominal=family==Family.WALL_RUN?.55:family==Family.WALL_CLIMB?.14:
                family==Family.RUN?.7:family==Family.WALK?(key.equals("basic_run")?.28:.12):0;
        float cadence=loop(key)?TraversalMotionSelector.cadence(family==Family.WALL_RUN||family==Family.WALL_CLIMB?velocity.length():speed,nominal):1;
        if(!fresh)state.time+=Math.clamp(now-state.clock,0,.1)*cadence;
        state.clock=now;
        float duration=Math.max(.001f,selected.getClipTime());
        float sample=loop(key)?(float)(state.time%duration):(float)Math.min(state.time,duration-.0001);
        Pose pose=selected.getPoseInTime(Math.max(0,sample));
        // Smooth the source wrap interval without altering the rest of the native cycle.
        if(loop(key)&&sample>duration-.06f)pose=Pose.interpolatePose(pose,selected.getPoseInTime(0),(sample-duration+.06f)/.06f);
        if(family==Family.WALL_RUN)NativeWallPose.orientRun(pose,Armatures.BIPED.get(),right,velocity.y,entity.getBbWidth()*.5F);
        if(state.from!=null&&now-state.changed<.12)pose=Pose.interpolatePose(state.from,pose,(float)Math.clamp((now-state.changed)/.12,0,1));
        // Keep the blend in wall-local space; camera yaw must not change the wall pose.
        if(wallFamily())NativeWallPose.clearHead(pose,Armatures.BIPED.get(),entity.getBbWidth()*.5F);
        state.last=Pose.interpolatePose(pose,pose,0);
        if(wall!=null&&wallFamily())NativeWallPose.faceWall(pose,Armatures.BIPED.get(),patch.getModelMatrix(partial),wall.normalX(),wall.normalZ());
        return pose;
    }
    private boolean wallFamily(){return switch(family){case WALL_RUN,WALL_CLIMB,WALL_CLING,WALL_LEAP,WALL_MANTLE,WALL_VAULT->true;default->false;};}
    private boolean loop(String key){
        return family.loop&&!key.endsWith("brake")&&!key.equals("basic_turn");
    }
}
