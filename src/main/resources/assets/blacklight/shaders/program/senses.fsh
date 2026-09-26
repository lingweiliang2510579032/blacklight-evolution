#version 150
uniform sampler2D DiffuseSampler;
uniform float VisionMode;
uniform float Strength;
in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    float luma = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    float lifted = pow(max(luma, 0.012), 0.65);
    vec3 cold = mix(vec3(0.008, 0.012, 0.07), vec3(0.10, 0.27, 0.46), lifted);
    vec3 infected = mix(vec3(0.045, 0.013, 0.025), vec3(0.31, 0.28, 0.29), lifted);
    vec2 center = texCoord * 2.0 - 1.0;
    float vignette = 1.0 - 0.23 * smoothstep(0.25, 1.6, dot(center, center));
    vec3 sensed = (VisionMode < 1.5 ? cold : infected) * vignette;
    fragColor = vec4(mix(scene, sensed, clamp(Strength, 0.0, 1.0)), 1.0);
}
