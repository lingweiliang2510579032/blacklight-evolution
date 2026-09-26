package com.codex.blacklight.client.renderer;

import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import com.codex.blacklight.mixin.client.AgeableModelAccessor;
import com.codex.blacklight.mixin.client.ModelPartChildrenAccessor;
import java.util.*;

/** Semantic parts survive posing: never infer an arm from a world-space cut plane. */
public final class ConsumeAnatomy {
    public static final int BODY=0,HEAD=1,RIGHT_ARM=2,LEFT_ARM=3,RIGHT_LEG=4,LEFT_LEG=5,TAIL=6,COUNT=7;
    private ConsumeAnatomy(){}
    public interface Consumer {
        void anatomy(int part);
        void anatomy(float[] weights);
        void anatomy(ModelPart part);
    }
    public static int named(String name,int fallback){
        String n=name.toLowerCase(Locale.ROOT);
        boolean left=n.contains("left")||n.endsWith("_l"),right=n.contains("right")||n.endsWith("_r");
        if(n.contains("head")||n.contains("jaw")||n.equals("hat")||n.contains("nose")||n.contains("ear"))return HEAD;
        if(n.contains("tail"))return TAIL;
        if(n.equals("arms"))return RIGHT_ARM; // Villager's folded arms are one connected model part.
        if(n.contains("arm")||n.contains("hand")||n.contains("elbow")||n.contains("shoulder")||n.contains("wing")||(n.contains("front")&&n.contains("leg")))return left?LEFT_ARM:right?RIGHT_ARM:fallback;
        if(n.contains("leg")||n.contains("thigh")||n.contains("shin")||n.contains("knee")||n.contains("foot")||n.contains("ankle"))return left?LEFT_LEG:right?RIGHT_LEG:fallback;
        if(n.equals("body")||n.contains("torso")||n.contains("chest")||n.contains("spine")||n.contains("pelvis")||n.equals("hips"))return BODY;
        return fallback;
    }
    public static Map<ModelPart,Integer> model(EntityModel<?> model){
        Map<ModelPart,Integer> parts=new IdentityHashMap<>();
        if(model instanceof HumanoidModel<?> h){
            tree(parts,h.body,BODY);tree(parts,h.head,HEAD);tree(parts,h.hat,HEAD);
            tree(parts,h.rightArm,RIGHT_ARM);tree(parts,h.leftArm,LEFT_ARM);tree(parts,h.rightLeg,RIGHT_LEG);tree(parts,h.leftLeg,LEFT_LEG);
        }else if(model instanceof HierarchicalModel<?> h)tree(parts,h.root(),BODY);
        else if(model instanceof AgeableModelAccessor a){
            for(var head:a.blacklight$heads())tree(parts,head,HEAD);
            int[] ids={BODY,RIGHT_LEG,LEFT_LEG,RIGHT_ARM,LEFT_ARM,TAIL};int i=0;
            for(var body:a.blacklight$body())tree(parts,body,ids[Math.min(i++,ids.length-1)]);
        }
        return parts;
    }
    private static void tree(Map<ModelPart,Integer> out,ModelPart p,int id){
        out.put(p,id);
        ((ModelPartChildrenAccessor)(Object)p).blacklight$children().forEach((name,child)->tree(out,child,named(name,id)));
    }
}
