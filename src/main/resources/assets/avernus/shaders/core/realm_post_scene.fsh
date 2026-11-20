#version 150

#moj_import <avernus:realm_noise.glsl>
#moj_import <avernus:realm_space.glsl>

// 全分辨率场景通道：热浪与光幕折射（中档起）、调色、合成降分辨率的氛围与光幕、地表环；输出 (rgb, 发光遮罩)。

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D FogSampler;
uniform sampler2D FxSampler;
uniform sampler2D NoiseSampler;

uniform mat4 InvViewProj;
uniform int Downscale;
uniform float Time;
uniform int Quality;
uniform float Strength;
uniform int Atmosphere;
uniform float SurfaceY;
uniform vec3 Tint;
uniform vec3 Glow;
uniform vec3 FogTint;
uniform float FogDistance;
uniform vec4 Bounds;
uniform vec2 Source;
uniform mat4 RealmShape;
uniform mat4 RealmSource;
uniform mat4 RealmLook;
uniform int RealmCount;

in vec2 texCoord;

out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

// 关档：旧版染色 + 线性距离雾 + 地表细亮环。
vec3 legacy(vec3 base, vec3 pos, bool sky) {
    float luma = dot(base, LUMA);
    vec3 color = mix(base, luma * Tint * 1.6, 0.35);
    float fog = sky ? 0.55 : smoothstep(0.0, FogDistance, length(pos)) * 0.65;
    color = mix(color, FogTint, fog);
    if (Bounds.w > 0.0 && !sky) {
        float ring = abs(length(pos.xz - Source) - Bounds.w);
        float shimmer = 0.75 + 0.25 * sin(Time * 6.0 + length(pos.xz) * 0.5);
        color += Tint * exp(-ring * ring * 0.5) * 0.6 * shimmer;
    }
    return mix(base, color, Strength);
}

// 旧版亮度染色为基底，暗部偏雾色、亮部偏辉光色，略增对比。
vec3 grade(vec3 base) {
    float luma = dot(base, LUMA);
    vec3 color = mix(base, luma * Tint * 1.6, 0.35);
    color += FogTint * (1.0 - luma) * (1.0 - luma) * 0.35;
    color = mix(color, color * (0.6 + Glow * 0.8), smoothstep(0.5, 1.0, luma) * 0.3);
    return max((color - 0.45) * 1.08 + 0.45, vec3(0.0));
}

// 降分辨率结果上采样：低档直接双线性（光幕已并入 fog），更高档按视距加权以免轮廓处渗色。
void upsample(float dist, out vec4 fog, out vec3 fx) {
    ivec2 size = textureSize(FogSampler, 0);
    vec2 p = (gl_FragCoord.xy - 0.5) / float(Downscale);
    if (Quality == 1) {
        fog = texture(FogSampler, (p + 0.5) / vec2(size));
        fx = vec3(0.0);
        return;
    }
    ivec2 origin = ivec2(floor(p));
    vec2 f = p - vec2(origin);
    fog = vec4(0.0);
    fx = vec3(0.0);
    float total = 0.0;
    for (int j = 0; j < 2; j++) {
        for (int i = 0; i < 2; i++) {
            ivec2 t = min(origin + ivec2(i, j), size - 1);
            vec4 light = texelFetch(FxSampler, t, 0);
            float bilinear = (i == 0 ? 1.0 - f.x : f.x) * (j == 0 ? 1.0 - f.y : f.y);
            float rel = abs(light.a - dist) / dist;
            float w = bilinear * (0.02 + exp(-rel * rel * 400.0));
            fog += texelFetch(FogSampler, t, 0) * w;
            fx += light.rgb * w;
            total += w;
        }
    }
    fog /= max(total, 1.0e-5);
    fx /= max(total, 1.0e-5);
}

// 屏幕扭曲：炼狱贴地热浪（随距离增强），光幕附近按其亮度折射。
vec2 distortion(vec3 pos, bool sky, vec3 fx) {
    vec2 wobble = vec2(realm_tnoise(NoiseSampler, texCoord * vec2(60.0, 30.0) + vec2(0.0, -Time * 2.5)),
                       realm_tnoise(NoiseSampler, texCoord * vec2(60.0, 30.0) + vec2(41.0, -Time * 2.1))) - 0.5;
    float heat = 0.0;
    if (Atmosphere == 1 && !sky) {
        float h = pos.y - SurfaceY;
        heat = 0.006 * Strength * smoothstep(4.0, 40.0, length(pos)) * exp(-max(h, 0.0) / 6.0);
    }
    return wobble * (heat + clamp(dot(fx, LUMA), 0.0, 1.0) * 0.012);
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    bool sky = depth >= 1.0;
    vec3 pos = realm_view_pos(InvViewProj, texCoord, depth);
    if (Quality == 0) {
        fragColor = vec4(legacy(texture(DiffuseSampler, texCoord).rgb, pos, sky), 0.0);
        return;
    }
    vec4 fog;
    vec3 fx;
    upsample(sky ? REALM_SKY_DISTANCE : length(pos), fog, fx);
    vec2 uv = texCoord;
    if (Quality >= 2) uv += distortion(pos, sky, fx);
    vec3 base = texture(DiffuseSampler, uv).rgb;
    vec3 color = mix(base, grade(base), Strength) * fog.a + fog.rgb;

    // 地表环与波前后方的余辉（细线，保留全分辨率）；footprint 为一个像素覆盖的水平距离
    float footprint = min(length(fwidth(pos.xz)), 8.0);
    for (int i = 0; i < 4; i++) {
        if (i >= RealmCount || sky) break;
        vec4 shape = RealmShape[i];
        vec4 src = RealmSource[i];
        float h = pos.y - src.z;
        if (shape.w <= 0.0 || abs(h) >= 2.5 || length(pos.xz - shape.xy) > shape.z + 0.5) continue;
        float flow = realm_flow(RealmLook[i]);
        float delta = length(pos.xz - src.xy) - shape.w;
        // 环宽不小于像素足迹，远处变宽变淡而不是时有时无
        float width = max(1.0, footprint);
        float ring = exp(-delta * delta * 0.5 / (width * width)) / width * (0.75 + 0.25 * sin(Time * 6.0 + length(pos.xz) * 0.5));
        float trail = flow > 0.0 && delta < 0.0 ? exp(delta / 6.0) : flow < 0.0 && delta > 0.0 ? exp(-delta / 6.0) : 0.0;
        fx += RealmLook[i].rgb * (ring * 0.9 + trail * 0.3) * (1.0 - abs(h) / 2.5);
    }
    fragColor = vec4(color + fx, clamp(dot(fx, LUMA), 0.0, 1.0));
}
