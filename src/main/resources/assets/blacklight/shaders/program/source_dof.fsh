#version 150
uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform vec2 InSize;
uniform float Strength;
uniform float FocusDistance;
uniform float FocusRange;
uniform float Aperture;
uniform float NearPlane;
uniform float FarPlane;
in vec2 texCoord;
out vec4 fragColor;
float distanceAt(vec2 uv) {
    float z=texture(DepthSampler,uv).r*2.0-1.0;
    return 2.0*NearPlane*FarPlane/max(0.0001,FarPlane+NearPlane-z*(FarPlane-NearPlane));
}
void main() {
    float depth=distanceAt(texCoord);
    float coc=clamp((abs(depth-FocusDistance)-FocusRange*0.5)/max(FocusRange,0.1),0.0,1.0);
    float radius=coc*clamp(Aperture,0.0,4.0)*2.0*Strength;
    vec4 centre=texture(DiffuseSampler,texCoord);
    vec4 sum=centre;float total=1.0;
    for(int i=0;i<12;i++) {
        float angle=float(i)*2.39996323;
        vec2 uv=clamp(texCoord+vec2(cos(angle),sin(angle))*sqrt((float(i)+0.5)/12.0)*radius/InSize,vec2(0.0),vec2(1.0));
        // Reject foreground samples crossing a depth discontinuity.
        float sampleDepth=distanceAt(uv);
        float weight=exp(-abs(sampleDepth-depth)/max(0.25,depth*0.1));
        sum+=texture(DiffuseSampler,uv)*weight;total+=weight;
    }
    fragColor=vec4((sum/total).rgb,centre.a);
}
