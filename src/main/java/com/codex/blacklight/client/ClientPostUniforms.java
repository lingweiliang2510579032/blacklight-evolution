package com.codex.blacklight.client;

import com.codex.blacklight.mixin.client.PostChainAccess;
import net.minecraft.client.renderer.PostChain;

public final class ClientPostUniforms {
    private ClientPostUniforms() {}
    public static void set(PostChain chain, String name, float value) {
        for (var pass : ((PostChainAccess) chain).blacklight$passes())
            pass.getEffect().safeGetUniform(name).set(value);
    }
}
