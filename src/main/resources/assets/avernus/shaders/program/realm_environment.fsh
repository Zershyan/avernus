#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

uniform mat4 InvViewProj;
uniform vec3 Tint;
uniform vec3 FogTint;
uniform float Strength;
uniform float FogDistance;
uniform vec2 WaveSource;
uniform float WaveRadius;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 base = texture(DiffuseSampler, texCoord);
    float depth = texture(DepthSampler, texCoord).r;
    vec4 world = InvViewProj * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 pos = world.xyz / world.w;
    bool sky = depth >= 1.0;

    float luma = dot(base.rgb, vec3(0.299, 0.587, 0.114));
    vec3 color = mix(base.rgb, luma * Tint * 1.6, 0.35);
    float fog = sky ? 0.55 : smoothstep(0.0, FogDistance, length(pos)) * 0.65;
    color = mix(color, FogTint, fog);

    if (WaveRadius > 0.0 && !sky) {
        float ring = abs(length(pos.xz - WaveSource) - WaveRadius);
        float shimmer = 0.75 + 0.25 * sin(Time * 0.3 + length(pos.xz) * 0.5);
        color += Tint * exp(-ring * ring * 0.5) * 0.6 * shimmer;
    }
    fragColor = vec4(mix(base.rgb, color, Strength), 1.0);
}
