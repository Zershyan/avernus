#version 150

#moj_import <avernus:realm_noise.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D BloomSampler;

uniform int Quality;
uniform float Strength;
uniform int Atmosphere;
uniform float Time;
uniform vec4 Flash;

in vec2 texCoord;

out vec4 fragColor;

// 软肩色调映射：0.8 以下恒等，以上平滑压到 1。
vec3 shoulder(vec3 c) {
    vec3 over = max(c - 0.8, vec3(0.0));
    return min(c, vec3(0.8)) + 0.2 * (1.0 - exp(-over / 0.2));
}

void main() {
    vec3 color = texture(SceneSampler, texCoord).rgb;
    if (Quality == 0) {
        fragColor = vec4(color, 1.0);
        return;
    }
    vec2 center = texCoord - 0.5;
    if (Quality >= 3) {
        float chroma = 0.0015 * Strength + 0.006 * Flash.a;
        color.r = texture(SceneSampler, texCoord + center * chroma).r;
        color.b = texture(SceneSampler, texCoord - center * chroma).b;
    }
    if (Quality >= 2) {
        float style = Atmosphere == 1 ? 1.0 : Atmosphere == 2 ? 0.7 : 0.8;
        color += texture(BloomSampler, texCoord).rgb * style * (0.45 + 0.1 * float(Quality));
    }
    float vignetteAmount = (Atmosphere == 1 ? 0.3 : Atmosphere == 2 ? 0.25 : 0.35) * Strength;
    color *= 1.0 - vignetteAmount * smoothstep(0.25, 0.75, length(center) * 1.2);
    color = mix(color, shoulder(color), Strength);
    if (Quality >= 3) {
        color += (realm_hash(gl_FragCoord.xy + fract(Time * 7.0) * 517.0) - 0.5) * 0.025 * Strength;
    }
    fragColor = vec4(mix(color, Flash.rgb, Flash.a * 0.75), 1.0);
}
