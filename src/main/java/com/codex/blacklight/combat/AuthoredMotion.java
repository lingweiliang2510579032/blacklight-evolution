package com.codex.blacklight.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Project-authored clips shipped independently of optional private motion imports. */
final class AuthoredMotion {
    private AuthoredMotion() {}
    static Optional<JsonObject> find(String id) {
        if (!id.equals("claw_pounce") && !id.equals("claw_pounce_charge")) return Optional.empty();
        String path="/assets/blacklight/authored_motions/"+id+".json";
        try (var stream=AuthoredMotion.class.getResourceAsStream(path)) {
            if(stream==null)throw new IllegalStateException("Missing authored motion: "+path);
            return Optional.of(JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot load authored motion: "+path,e); }
    }
}
