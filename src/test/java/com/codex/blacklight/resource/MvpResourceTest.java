package com.codex.blacklight.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvpResourceTest {
    private static JsonObject dependencyArmature;
    @Test
    void authoredActionsHaveCompleteNormalizedTracksAndMatchServerDurations() throws IOException {
        Map<String, Double> clips = Map.ofEntries(
                Map.entry("action/air_dash", 0.6),
                Map.entry("action/consume", 2.3),
                Map.entry("action/consume_normal", 2.3), Map.entry("action/consume_hammer", 2.8),
                Map.entry("action/consume_blade", 2.4), Map.entry("action/consume_whip", 1.9),
                Map.entry("action/grab_reach", .6), Map.entry("action/grab_hold", 2.0),
                Map.entry("action/grab_throw", .7), Map.entry("action/grab_whip", 1.8),
                Map.entry("action/transform_claws", 1.2),
                Map.entry("combat/claw_auto1", 0.7),
                Map.entry("combat/claw_auto2", 0.8),
                Map.entry("combat/claw_auto3", 1.1),
                Map.entry("combat/claw_dashing_slice", 1.0),
                Map.entry("combat/claw_groundspike", 2.2),
                Map.entry("combat/claw_sprint_attack", 0.8),
                Map.entry("living/claw_idle", 1.6),
                Map.entry("living/claw_sprint", 0.54),
                Map.entry("living/claw_walk", 0.9),
                Map.entry("living/landing_devastating", 0.62),
                Map.entry("living/landing_heavy", 0.44),
                Map.entry("living/landing_light", 0.28),
                Map.entry("living/prototype_glide", 0.7),
                Map.entry("living/prototype_jump_charge", 2.05),
                Map.entry("living/prototype_jump_fall", 0.2),
                Map.entry("living/prototype_jump_rise", 0.2),
                Map.entry("living/wall_run", 0.48),
                Map.entry("living/wall_climb", 0.96),
                Map.entry("living/wall_cling", 1.0),
                Map.entry("living/wall_leap", 0.4));
        for (var entry : clips.entrySet()) {
            var data = tracks(readAnimation("biped/" + entry.getKey() + ".json"));
            assertEquals(20, data.size(), entry.getKey() + " must animate the complete skeleton");
            for (var track : data.values()) {
                JsonArray times = track.getAsJsonArray("time");
                assertEquals(entry.getValue(), times.get(times.size() - 1).getAsDouble(), 1e-6,
                        entry.getKey() + " must finish with its server action");
                double previous = -1;
                for (var t : times) {
                    assertTrue(t.getAsDouble() > previous, "key times must increase");
                    previous = t.getAsDouble();
                }
                for (var f : track.getAsJsonArray("transform")) {
                    double norm = 0;
                    for (var component : f.getAsJsonObject().getAsJsonArray("rot")) {
                        double v = component.getAsDouble();
                        assertTrue(Double.isFinite(v));
                        norm += v * v;
                    }
                    assertEquals(1, norm, 1e-5, "non-unit quaternion changes limb orientation/scale");
                }
            }
        }
    }

    @Test
    void wallRunUsesBentRunningArmsWhileClimbReachesUpAndClingHoldsStill() throws IOException {
        var run=readAnimation("biped/living/wall_run.json");
        var climb=readAnimation("biped/living/wall_climb.json");
        for (double time:new double[]{0,.12,.24,.36}) {
            var running=worldPose(run,time);var climbing=worldPose(climb,time*2);
            for(String side:new String[]{"R","L"}) {
                double[] wrist=point(running.get("Hand_"+side),0,.272858,0);
                assertTrue(wrist[2]<running.get("Arm_"+side)[2][3]-.15,"running arms stay bent below shoulders");
                assertTrue(point(climbing.get("Hand_"+side),0,.272858,0)[2]>wrist[2]+.12,"climbing hands reach higher");
            }
        }
        for(var track:tracks(readAnimation("biped/living/wall_cling.json")).values()) {
            var frames=track.getAsJsonArray("transform");
            for(var frame:frames) assertEquals(frames.get(0),frame,"cling must not keep cycling the limbs");
        }
    }

    @Test
    void everyServerTimedMvpActionHasAnEpicFightAnimation() {
        assertResource("biped/combat/claw_auto1.json");
        assertResource("biped/combat/claw_auto2.json");
        assertResource("biped/combat/claw_auto3.json");
        assertResource("biped/combat/claw_groundspike.json");
        assertResource("biped/combat/claw_dashing_slice.json");
        assertResource("biped/action/transform_claws.json");
        assertResource("biped/action/air_dash.json");
        assertResource("biped/living/prototype_jump_charge.json");
        assertResource("biped/living/prototype_jump_rise.json");
        assertResource("biped/living/prototype_jump_fall.json");
        assertResource("biped/living/landing_light.json");
        assertResource("biped/living/landing_heavy.json");
        assertResource("biped/living/landing_devastating.json");
    }

    @Test
    void groundedCrouchesKeepTheFeetAtTheFloorInsteadOfSinkingTheWholeBody() throws IOException {
        for (String path : new String[] {"combat/claw_groundspike", "living/prototype_jump_charge",
                "living/landing_light", "living/landing_heavy", "living/landing_devastating"}) {
            var clip=readAnimation("biped/"+path+".json");
            var times=tracks(clip).get("Root").getAsJsonArray("time");
            double contact=path.endsWith("light")?.07:path.endsWith("heavy")?.12:path.endsWith("devastating")?.18:0;
            for (int i=0;i<times.size();i++) {
                double t=times.get(i).getAsDouble(); if(t+1e-6<contact) continue;
                var pose=worldPose(clip,t);
                for(String side:new String[]{"R","L"})
                    assertEquals(.015,point(pose.get("Leg_"+side),0,.37472,0)[2],.025,path+" foot "+side+" at "+t);
            }
        }
    }

    @Test
    void virusAmpouleHasAThreeDimensionalItemModel() {
        assertNotNull(MvpResourceTest.class.getResource(
                "/assets/blacklight/models/item/blacklight_sample.json"));
        assertNotNull(MvpResourceTest.class.getResource(
                "/assets/blacklight/textures/item/biomass.png"));
    }

    @Test
    void importedImpactParticlesShipWithTexturesDefinitionsAndLicense() {
        for (String name : new String[] {"biomass_slash", "destruction_burst", "impact_smoke"}) {
            assertNotNull(MvpResourceTest.class.getResource(
                    "/assets/blacklight/particles/" + name + ".json"), name + " definition");
            assertNotNull(MvpResourceTest.class.getResource(
                    "/assets/blacklight/textures/particle/" + name + ".png"), name + " texture");
        }
        assertNotNull(MvpResourceTest.class.getResource(
                "/META-INF/third-party/kenney-particle-pack-CC0.txt"));
    }

    @Test
    void sprintShockwaveShipsWithItsHorizontalRingTexture() {
        assertNotNull(MvpResourceTest.class.getResource(
                "/assets/blacklight/particles/sprint_shockwave.json"));
        assertNotNull(MvpResourceTest.class.getResource(
                "/assets/blacklight/textures/particle/sprint_shockwave.png"));
    }

    @Test
    void velocityCorrectedSpeedPostEffectShipsAsACompleteLicensedChain() throws IOException {
        JsonObject chain = readJson("/assets/blacklight/shaders/post/prototype_speed.json");
        JsonArray passes = chain.getAsJsonArray("passes");
        assertEquals(2, passes.size(), "speed effect must render off-screen then blit to main");
        assertEquals("blacklight:prototype_speed",
                passes.get(0).getAsJsonObject().get("name").getAsString());
        assertEquals("minecraft:main",
                passes.get(1).getAsJsonObject().get("outtarget").getAsString());

        JsonObject program = readJson("/assets/blacklight/shaders/program/prototype_speed.json");
        Set<String> uniforms = new java.util.HashSet<>();
        for (JsonElement element : program.getAsJsonArray("uniforms")) {
            uniforms.add(element.getAsJsonObject().get("name").getAsString());
        }
        assertTrue(uniforms.containsAll(Set.of(
                "ProjMat", "InSize", "OutSize", "Time", "Intensity", "BiasX", "BiasY")));
        assertNotNull(MvpResourceTest.class.getResource(
                "/assets/blacklight/shaders/program/prototype_speed.fsh"));
        assertNotNull(MvpResourceTest.class.getResource(
                "/META-INF/third-party/anime-speed-lines-MIT.txt"));
    }

    @Test
    void prototypeWalkAndRunAnimateTheCompleteBipedWithoutRootTranslation() throws IOException {
        assertCompleteLocomotion("biped/living/claw_walk.json");
        assertCompleteLocomotion("biped/living/claw_sprint.json");
        assertCompleteLocomotion("biped/living/wall_run.json");
    }

    @Test
    void landingAnimationsNeverTranslateRootThroughTheWorld() throws IOException {
        for (String path : new String[] {
                "biped/living/landing_light.json",
                "biped/living/landing_heavy.json",
                "biped/living/landing_devastating.json"}) {
            JsonArray root = tracks(readAnimation(path)).get("Root").getAsJsonArray("transform");
            for (JsonElement frame : root) {
                JsonArray location = frame.getAsJsonObject().getAsJsonArray("loc");
                JsonArray rotation = frame.getAsJsonObject().getAsJsonArray("rot");
                assertEquals(0.0D, location.get(0).getAsDouble(), 0.00001D);
                assertEquals(0.0D, location.get(2).getAsDouble(), 0.00001D);
                assertIdentity(rotation);
            }
        }
    }

    @Test
    void chargedJumpCrouchesForwardSweepsBothArmsBackwardAndKeepsRootUpright() throws IOException {
        var clip=readAnimation("biped/living/prototype_jump_charge.json");
        var start=worldPose(clip,0);var end=worldPose(clip,2.05);
        assertTrue(end.get("Root")[2][3]<start.get("Root")[2][3]-.22,"charge must compress the legs");
        assertTrue(end.get("Chest")[1][3] > end.get("Root")[1][3] + .08, "the skin faces +Y: charge must lean forward, not backward");
        assertTrue(end.get("Head")[1][3] > end.get("Root")[1][3] + .20, "the head must travel toward the front of the skin while crouching");
        for(String side:new String[]{"R","L"}) {
            double[] wrist=point(end.get("Hand_"+side),0,.272858,0);
            assertTrue(wrist[1]<end.get("Arm_"+side)[1][3]-.15,"arms must sweep behind the shoulders");
        }
        for(var frame:tracks(clip).get("Root").getAsJsonArray("transform"))
            assertIdentity(frame.getAsJsonObject().getAsJsonArray("rot"));
    }

    @Test
    void groundSpikeKeepsThePlayerUprightPlantsOnlyTheRightClawAndEruptsUpward()
            throws IOException {
        var clip=readAnimation("biped/combat/claw_groundspike.json");
        var wind=worldPose(clip,.24);var plant=worldPose(clip,.55);
        double[] raised=point(wind.get("Hand_R"),0,.272858,0);
        double[] planted=point(plant.get("Hand_R"),0,.272858,0);
        double[] free=point(plant.get("Hand_L"),0,.272858,0);
        assertTrue(raised[2]>planted[2]+.8,"right claw must drive down from a raised windup");
        assertTrue(free[2]>planted[2]+.25,"free claw balances above the planted claw");
        for(var frame:tracks(clip).get("Root").getAsJsonArray("transform"))
            assertIdentity(frame.getAsJsonObject().getAsJsonArray("rot"));

        JsonObject geometry = readJson("/assets/blacklight/geo/ground_spike.geo.json");
        JsonArray spikeBones = geometry.getAsJsonArray("minecraft:geometry").get(0)
                .getAsJsonObject().getAsJsonArray("bones");
        double tallest = 0.0D;
        JsonObject eruption = readJson("/assets/blacklight/animations/ground_spike.animation.json")
                .getAsJsonObject("animations")
                .getAsJsonObject("animation.blacklight.ground_spike.erupt");
        int animatedSpires = 0;
        for (JsonElement element : spikeBones) {
            JsonObject bone = element.getAsJsonObject();
            if (!bone.has("cubes")) continue;
            double top = 0.0D;
            for (JsonElement elementCube : bone.getAsJsonArray("cubes")) {
                JsonObject cube = elementCube.getAsJsonObject();
                // A segmented spire must be judged by its whole height, not one tier.
                top = Math.max(top, cube.getAsJsonArray("origin").get(1).getAsDouble()
                        + cube.getAsJsonArray("size").get(1).getAsDouble());
            }
            tallest = Math.max(tallest, top);
            if (top < 32.0D) continue;
            JsonObject position = eruption.getAsJsonObject("bones")
                    .getAsJsonObject(bone.get("name").getAsString()).getAsJsonObject("position");
            assertTrue(position.getAsJsonArray("0.0").get(1).getAsDouble() + top < 1.0D,
                    "each spire must start below the surface");
            assertTrue(position.entrySet().stream().anyMatch(frame ->
                            frame.getValue().getAsJsonArray().get(1).getAsDouble() >= 0.0D),
                    "each spire must emerge fully above ground");
            assertTrue(position.getAsJsonArray("1.1").get(1).getAsDouble() + top < 1.0D,
                    "each spire must retract before the entity expires");
            animatedSpires++;
        }
        assertTrue(tallest >= 64.0D, "groundspike must be visibly tall");
        assertTrue(animatedSpires >= 5, "the full cluster must animate, not just its base");
        assertEquals(1.1D, eruption.get("animation_length").getAsDouble(), 0.00001D,
                "the eruption must retain the server's 22-tick lifetime");
    }

    private static void assertCompleteLocomotion(String path) throws IOException {
        JsonObject animation = readAnimation(path);
        Map<String, JsonObject> tracks = tracks(animation);

        Set<String> completeBiped = Set.of(
                "Root", "Thigh_R", "Leg_R", "Knee_R", "Thigh_L", "Leg_L", "Knee_L",
                "Torso", "Chest", "Head",
                "Shoulder_R", "Arm_R", "Elbow_R", "Hand_R", "Tool_R",
                "Shoulder_L", "Arm_L", "Elbow_L", "Hand_L", "Tool_L");
        assertTrue(tracks.keySet().containsAll(completeBiped),
                path + " missing joints " + completeBiped.stream()
                        .filter(joint -> !tracks.containsKey(joint)).toList());

        JsonArray rootFrames = tracks.get("Root").getAsJsonArray("transform");
        for (JsonElement frame : rootFrames) {
            JsonArray location = frame.getAsJsonObject().getAsJsonArray("loc");
            JsonArray rotation = frame.getAsJsonObject().getAsJsonArray("rot");
            assertEquals(0.0D, location.get(0).getAsDouble(), 0.00001D,
                    path + " must not translate sideways");
            assertEquals(0.0D, location.get(2).getAsDouble(), 0.00001D,
                    path + " must not translate through the world");
            assertEquals(0,rotation.get(1).getAsDouble(),.001,"locomotion pelvis must not pitch");
            assertEquals(0,rotation.get(3).getAsDouble(),.001,"locomotion pelvis must not roll");
        }

        JsonArray rightThigh = tracks.get("Thigh_R").getAsJsonArray("transform");
        JsonArray leftThigh = tracks.get("Thigh_L").getAsJsonArray("transform");
        assertNotEquals(rightThigh.get(0).toString(), leftThigh.get(0).toString(),
                path + " legs must be half a stride out of phase");
        for(String joint:completeBiped) {
            var frames=tracks.get(joint).getAsJsonArray("transform");
            for(String property:new String[]{"loc","rot","sca"}) {
                var first=frames.get(0).getAsJsonObject().getAsJsonArray(property);
                var last=frames.get(frames.size()-1).getAsJsonObject().getAsJsonArray(property);
                for(int axis=0;axis<first.size();axis++) assertEquals(first.get(axis).getAsDouble(),last.get(axis).getAsDouble(),.0001,path+" loop "+joint);
            }
        }
    }

    private static void assertIdentity(JsonArray q) {
        for(int i=0;i<4;i++) assertEquals(i==0?1:0,q.get(i).getAsDouble(),.00001);
    }

    @Test
    void glideSpreadsTheWristsOutsideTheShoulders() throws IOException {
        var pose = worldPose(readAnimation("biped/living/prototype_glide.json"), .35);
        double[] right = point(pose.get("Hand_R"), 0, .272858, 0);
        double[] left = point(pose.get("Hand_L"), 0, .272858, 0);
        assertTrue(right[0] > .80 && left[0] < -.80,
                "glide must spread both arms rather than fold them through the torso");
    }

    @Test
    void clawThumbsMatchTheirAnatomicalHands() throws IOException {
        var mesh = readJson("/assets/blacklight/models/claw_blender.json");
        for (String side : new String[]{"R", "L"}) {
            double[] palm = clawPartCenter(mesh, side + " / palm");
            double[] thumb = clawPartCenter(mesh, side + " / thumb root");
            double[] finger = clawPartCenter(mesh, side + " / knuckle 2");
            double[] back = clawPartCenter(mesh, side + " / palm ridge 2");
            for (int i=0;i<3;i++) {thumb[i]-=palm[i];finger[i]-=palm[i];back[i]=palm[i]-back[i];}
            // A right hand has thumb x fingers pointing into its palmar surface;
            // a left hand has the opposite chirality. Read the actual mesh faces.
            double chirality=(thumb[1]*finger[2]-thumb[2]*finger[1])*back[0]
                    +(thumb[2]*finger[0]-thumb[0]*finger[2])*back[1]
                    +(thumb[0]*finger[1]-thumb[1]*finger[0])*back[2];
            assertTrue((side.equals("R") ? chirality : -chirality) > .0005,
                    side + " claw thumb is on the wrong anatomical side");
        }
    }

    @Test
    void wallAndMorphActionsKeepTheActualClawMeshClearOfTheHeadThroughoutEachClip() throws IOException {
        var mesh=readJson("/assets/blacklight/models/claw_blender.json").getAsJsonObject("joints");
        for(String name:new String[]{"living/wall_run","living/wall_climb","living/wall_cling","living/wall_leap","action/transform_claws","action/consume","action/grab_reach","action/grab_hold","action/grab_throw"}) {
            var clip=readAnimation("biped/"+name+".json");
            for(var time:tracks(clip).get("Head").getAsJsonArray("time")) {
                var pose=worldPose(clip,time.getAsDouble());var head=pose.get("Head");
                for(String side:new String[]{"R","L"}) {
                    var hand=pose.get("Hand_"+side);
                    for(var face:mesh.getAsJsonArray("Hand_"+side)) for(var vertex:face.getAsJsonObject().getAsJsonArray("p")) {
                        var v=vertex.getAsJsonArray();var world=point(hand,v.get(0).getAsDouble(),v.get(1).getAsDouble(),v.get(2).getAsDouble());
                        double[] local=new double[3];
                        for(int axis=0;axis<3;axis++)for(int k=0;k<3;k++)local[axis]+=head[k][axis]*(world[k]-head[k][3]);
                        // Minecraft's 8-pixel head occupies +/- .25 around local Y=.25.
                        double dx=Math.max(0,Math.abs(local[0])-.25),dy=Math.max(0,Math.abs(local[1]-.25)-.25),dz=Math.max(0,Math.abs(local[2])-.25);
                        assertTrue(dx*dx+dy*dy+dz*dz>=.06*.06,name+" "+side+" claw approaches head at "+time);
                    }
                }
            }
        }
    }

    @Test
    void newConsumeFormsKeepTheirOwnMeshesClearOfTheHead() throws IOException {
        var failures = new java.util.ArrayList<String>();
        for (var form : Map.of("hammerfists", "hammer", "blade", "blade", "whipfist", "whip").entrySet()) {
            for(String name:new String[]{"action/consume_"+form.getValue(),"action/grab_reach","action/grab_hold","action/grab_throw",form.getKey().equals("whipfist")?"action/grab_whip":"action/grab_hold"}) {
            var mesh = readJson("/assets/blacklight/models/form_"+form.getKey()+".json").getAsJsonObject("joints");
            double min = 100; String closest = "";
            var clip=readAnimation("biped/"+name+".json");
            for(var time:tracks(clip).get("Head").getAsJsonArray("time")) {
                var pose=worldPose(clip,time.getAsDouble());var head=pose.get("Head");
                for(String side:new String[]{"R","L"}) {
                    if (!mesh.has("Hand_"+side)) continue;
                    var hand=pose.get("Hand_"+side);
                    for(var face:mesh.getAsJsonArray("Hand_"+side)) for(var vertex:face.getAsJsonObject().getAsJsonArray("p")) {
                        var v=vertex.getAsJsonArray();var world=point(hand,v.get(0).getAsDouble(),v.get(1).getAsDouble(),v.get(2).getAsDouble());
                        double[] local=new double[3];
                        for(int axis=0;axis<3;axis++)for(int k=0;k<3;k++)local[axis]+=head[k][axis]*(world[k]-head[k][3]);
                        // Minecraft's 8-pixel head occupies +/- .25 around local Y=.25.
                        double dx=Math.max(0,Math.abs(local[0])-.25),dy=Math.max(0,Math.abs(local[1]-.25)-.25),dz=Math.max(0,Math.abs(local[2])-.25);
                        double d = Math.sqrt(dx*dx+dy*dy+dz*dz);
                        if (d < min) { min=d; closest=name+" "+side+" at "+time; }
                    }
                }
            }
            if (min < .06) failures.add(closest+" clearance="+min);
            }
        }
        assertTrue(failures.isEmpty(), String.join("; ", failures));
    }

    private static double[] clawPartCenter(JsonObject mesh, String name) {
        var part=mesh.getAsJsonObject("parts").getAsJsonObject(name);
        assertEquals(name.substring(0,1),part.get("joint").getAsString().substring(5));
        var faces=mesh.getAsJsonObject("joints").getAsJsonArray(part.get("joint").getAsString());
        int start=part.get("start").getAsInt(),count=part.get("count").getAsInt();
        double[] sum=new double[3];
        for(int i=start;i<start+count;i++)for(var p:faces.get(i).getAsJsonObject().getAsJsonArray("p"))
            for(int axis=0;axis<3;axis++)sum[axis]+=p.getAsJsonArray().get(axis).getAsDouble()/(count*3);
        return sum;
    }

    // Independent forward kinematics on the shipped skeleton. Assertions use world
    // positions rather than key indices or the sign of one quaternion component.
    private static Map<String,double[][]> worldPose(JsonObject clip,double time) throws IOException {
        Map<String,double[][]> result=new HashMap<>();
        var skeleton=readJson("/epicfight-biped-armature.json");
        for(var root:skeleton.getAsJsonArray("hierarchy"))
            walkPose(root.getAsJsonObject(),identity(),tracks(clip),time,result);
        return result;
    }
    private static void walkPose(JsonObject bone,double[][] parent,Map<String,JsonObject> tracks,double time,Map<String,double[][]> out) {
        String name=bone.get("name").getAsString();double[][] rest=new double[4][4];
        var flat=bone.getAsJsonArray("transform");
        for(int i=0;i<16;i++)rest[i/4][i%4]=flat.get(i).getAsDouble();
        var tr=tracks.get(name);var times=tr.getAsJsonArray("time");int index=0;
        for(int i=1;i<times.size();i++)if(Math.abs(times.get(i).getAsDouble()-time)<Math.abs(times.get(index).getAsDouble()-time))index=i;
        var f=tr.getAsJsonArray("transform").get(index).getAsJsonObject();var q=f.getAsJsonArray("rot");
        // The loader conjugates XYZ, and OpenMatrix4f.fromQuaternion transposes
        // the conventional rotation matrix: the two operations cancel.
        double w=q.get(0).getAsDouble(),x=q.get(1).getAsDouble(),y=q.get(2).getAsDouble(),z=q.get(3).getAsDouble();
        double[][] local={{1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w),0},
            {2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w),0},
            {2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y),0},{0,0,0,1}};
        for(int i=0;i<3;i++) {
            local[i][3]=f.getAsJsonArray("loc").get(i).getAsDouble();
            for(int j=0;j<3;j++) local[i][j]*=f.getAsJsonArray("sca").get(j).getAsDouble();
        }
        double[][] world=multiply(multiply(parent,rest),local);out.put(name,world);
        for(var child:bone.getAsJsonArray("children"))walkPose(child.getAsJsonObject(),world,tracks,time,out);
    }
    private static double[][] identity(){return new double[][]{{1,0,0,0},{0,1,0,0},{0,0,1,0},{0,0,0,1}};}
    private static double[][] multiply(double[][] a,double[][] b){
        double[][] c=new double[4][4];for(int i=0;i<4;i++)for(int j=0;j<4;j++)for(int k=0;k<4;k++)c[i][j]+=a[i][k]*b[k][j];return c;
    }
    private static double[] point(double[][] m,double x,double y,double z){
        return new double[]{m[0][0]*x+m[0][1]*y+m[0][2]*z+m[0][3],m[1][0]*x+m[1][1]*y+m[1][2]*z+m[1][3],m[2][0]*x+m[2][1]*y+m[2][2]*z+m[2][3]};
    }

    private static JsonObject readAnimation(String path) throws IOException {
        return readJson("/assets/blacklight/animmodels/animations/" + path);
    }

    private static JsonObject readJson(String path) throws IOException {
        if (path.equals("/epicfight-biped-armature.json")) {
            if (dependencyArmature==null) {
                String dependency=System.getProperty("blacklight.test.epicFightJar");
                assertNotNull(dependency,"Run tests through Gradle to resolve the Epic Fight dependency");
                try(var zip=new java.util.zip.ZipFile(dependency)) {
                    var entry=zip.getEntry("assets/epicfight/animmodels/entity/biped.json");
                    assertNotNull(entry,"Epic Fight biped model is required for compatibility checks");
                    try(var reader=new InputStreamReader(zip.getInputStream(entry),StandardCharsets.UTF_8)) {
                        dependencyArmature=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("armature");
                    }
                }
            }
            return dependencyArmature;
        }
        var stream = MvpResourceTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Map<String, JsonObject> tracks(JsonObject animation) {
        Map<String, JsonObject> tracks = new HashMap<>();
        for (JsonElement element : animation.getAsJsonArray("animation")) {
            JsonObject track = element.getAsJsonObject();
            tracks.put(track.get("name").getAsString(), track);
            assertEquals(track.getAsJsonArray("time").size(),
                    track.getAsJsonArray("transform").size(),
                    track.get("name").getAsString() + " keyframe count");
        }
        return tracks;
    }

    private static void assertResource(String path) {
        assertNotNull(MvpResourceTest.class.getResource(
                "/assets/blacklight/animmodels/animations/" + path), path);
    }
}
