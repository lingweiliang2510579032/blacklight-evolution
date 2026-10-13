package com.codex.blacklight.client.renderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.codex.blacklight.combat.DevastatorEffectTimeline.*;
class BulletImpactMistTest {
    @Test void forgeShaderDescriptorsBindTheActualVertexFormat() throws Exception {
        for(String name:new String[]{"impact_mist","liquid_biomass","rigid_biomass"}){
            try(var input=getClass().getResourceAsStream("/assets/blacklight/shaders/core/"+name+".json")){
                assertNotNull(input);
                var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                assertTrue(json.has("attributes"),"1.20.1 skips glBindAttribLocation without an attributes array: "+name);
                assertTrue(json.has("blend"),"1.20.1 shader apply disables translucent RenderType blending without a blend node: "+name);
                assertEquals("srcalpha",json.getAsJsonObject("blend").get("srcrgb").getAsString());
                assertEquals("1-srcalpha",json.getAsJsonObject("blend").get("dstrgb").getAsString());
                var actual=new java.util.ArrayList<String>();json.getAsJsonArray("attributes").forEach(v->actual.add(v.getAsString()));
                assertEquals(name.equals("impact_mist")?java.util.List.of("Position","UV0","Color"):
                    java.util.List.of("Position","Color","UV0","UV1","UV2","Normal"),actual);
            }
        }
    }

    @Test void plumeStartsAfterItsDelayAndFadesWithoutFrameRandomness(){
        assertEquals(0,impactMistOpacity(0,.1));
        assertTrue(impactMistOpacity(.3,.1)>.8);
        assertTrue(impactMistOpacity(2,.1)<impactMistOpacity(1,.1));
        assertEquals(0,impactMistOpacity(3.2,.1));
        assertEquals(0,impactMistOpacity(Double.NaN,.1));
        assertEquals(0,impactMistOpacity(Double.POSITIVE_INFINITY,.1));
    }
    @Test void redClustersGrowWithHeightAndOnlyStrictlyOverFiftyAddsMist(){
        double radius=0,height=0;
        for(int h=0;h<=50;h++){
            var p=impact(h);assertFalse(p.mist());assertEquals(0,p.mistGain());
            assertTrue(p.clusterRadius()>radius);assertTrue(p.clusterHeight()>height);
            radius=p.clusterRadius();height=p.clusterHeight();
        }
        assertTrue(impact(Math.nextUp(50D)).mist());assertTrue(impact(80).mistGain()>impact(51).mistGain());
        assertFalse(impact(Double.NaN).mist());assertFalse(impact(Double.POSITIVE_INFINITY).mist());
        assertEquals(0,impactClusterOpacity(-1));assertTrue(impactClusterOpacity(.1)>.9);
        assertEquals(0,impactClusterOpacity(.8));assertEquals(0,impactClusterOpacity(Double.NaN));
    }
}
