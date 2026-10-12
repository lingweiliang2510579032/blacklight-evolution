package com.codex.blacklight.client.animation;

import com.codex.blacklight.client.model.BlockShieldModel;
import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeShieldItemTest {
    private static JsonObject data(){try(var in=NativeShieldItemTest.class.getResourceAsStream("/assets/blacklight/models/shield_block.json")){assertNotNull(in);return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private static Vector3f vector(JsonArray a){return new Vector3f(a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat());}
    private static List<Vector3f> points(){var result=new ArrayList<Vector3f>();for(var part:data().getAsJsonArray("parts"))for(var p:part.getAsJsonObject().getAsJsonArray("points"))result.add(vector(p.getAsJsonArray()));return result;}
    @Test void sourceShieldBasisMapsToTheBuiltinPlateWithoutChangingMeshDimensions(){
        var root=data();var frame=new BlockShieldModel().itemFrame();
        assertTrue(frame.transformDirection(vector(root.getAsJsonArray("right"))).equals(new Vector3f(1,0,0),.0002F));
        assertTrue(frame.transformDirection(vector(root.getAsJsonArray("up"))).equals(new Vector3f(0,-1,0),.0002F));
        assertTrue(frame.transformDirection(vector(root.getAsJsonArray("front"))).equals(new Vector3f(0,0,-1),.0002F));
        var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);
        for(var point:points()){var p=frame.transformPosition(point);min.min(p);max.max(p);}
        var center=min.add(max).mul(.5F);assertTrue(center.equals(new Vector3f(0,0,-1.5F/16F),.0002F));
    }
    @Test void nativeLeftShieldDisplayHasVisiblePlateAndCorrectActualDepthBounds(){
        var shield=new BlockShieldModel();
        // Native applyItemArmTransform and shield.json FIRST_PERSON_LEFT_HAND,
        // with ItemTransform's left-hand mirror and builtin ShieldModel's basis.
        var frame=new Matrix4f().translation(-.56F,-.52F,-.72F).translate(-10F/16F,0,-10F/16F)
                .rotateY((float)Math.toRadians(-180)).rotateZ((float)Math.toRadians(-5)).scale(1.25F)
                .translate(-.5F,-.5F,-.5F).scale(1,-1,-1).mul(shield.itemFrame());
        var projection=new Matrix4f().perspective((float)Math.toRadians(70),1280F/900,.05F,100);
        var measured=shield.projectedItem(frame,projection);float near=Float.POSITIVE_INFINITY,x=0,y=0;int visible=0;
        for(var point:points()){
            var p=frame.transformPosition(point);var clip=projection.transform(new Vector4f(p,1));near=Math.min(near,-p.z);
            x=Math.max(x,Math.abs(clip.x/clip.w));y=Math.max(y,Math.abs(clip.y/clip.w));
            if(clip.w>0&&Math.abs(clip.x/clip.w)<=1&&Math.abs(clip.y/clip.w)<=1)visible++;
        }
        assertTrue(near>.15F);assertTrue(visible>points().size()/10,"native item frame must display a usable shield surface");
        assertEquals(near,measured.nearestDepth(),.0002);assertEquals(x,measured.maximumProjectedX(),.0002);assertEquals(y,measured.maximumProjectedY(),.0002);
    }
}
