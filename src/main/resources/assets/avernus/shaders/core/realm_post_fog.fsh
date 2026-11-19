#version 150

#moj_import <avernus:realm_noise.glsl>
#moj_import <avernus:realm_space.glsl>
#moj_import <avernus:realm_sky.glsl>
#moj_import <avernus:realm_fx.glsl>

// 降分辨率氛围通道：输出 (内散射 rgb, 透射率 a)，合成为 color·a + rgb。
// 含当前领域的雾（低档解析高度雾，中档起体积步进）与程序化天空（已乘 Strength）、境外看领域时的雾穹；低档无泛光，光幕也并入这里。

uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;

uniform mat4 InvViewProj;
uniform vec3 CameraPos;
uniform int Downscale;
uniform int FogSteps;
uniform float Time;
uniform int Quality;
uniform float Strength;
uniform int Atmosphere;
uniform vec3 Tint;
uniform vec3 Glow;
uniform vec3 FogTint;
uniform float FogDistance;
uniform float FogDensity;
uniform float SurfaceY;
uniform vec4 Bounds;
uniform mat4 RealmShape;
uniform mat4 RealmSource;
uniform mat4 RealmLook;
uniform int RealmCount;
uniform vec4 Shock;

out vec4 fragColor;

// 雾层厚度（高度衰减尺度，格）。
float fog_scale() {
    return Atmosphere == 1 ? 9.0 : Atmosphere == 2 ? 16.0 : 5.0;
}

// 风格雾密度 0..1，p 为世界坐标（已取模）。
float style_density(vec3 p, int octaves) {
    if (Atmosphere == 1) {
        // 炼狱：翻滚上升的烟团
        return smoothstep(0.5, 0.8, realm_fbm3(NoiseSampler, p * vec3(0.13, 0.18, 0.13) + vec3(0.0, -Time * 0.45, Time * 0.06), octaves));
    }
    if (Atmosphere == 2) {
        // 寂静：水平薄片，竖直方向频率更高
        return smoothstep(0.4, 0.68, realm_fbm3(NoiseSampler, p * vec3(0.08, 0.45, 0.08) + vec3(Time * 0.08, 0.0, Time * 0.03), octaves));
    }
    // 幽冥：脊状丝缕，缓慢旋绕
    float n = realm_fbm3(NoiseSampler, p * vec3(0.14, 0.22, 0.14) + vec3(Time * 0.1, Time * 0.05, -Time * 0.08), octaves);
    return pow(1.0 - abs(2.0 * n - 1.0), 5.0);
}

// 体积雾：只在雾层（离地 3 倍厚度以内）里步进，步点向相机加密；步进段以外用解析高度雾补足远处。
// 返回 (散射 rgb, 透射率 a)，未乘 Strength。
vec4 volumetric(vec3 dir, float limit) {
    float scale = fog_scale();
    float rho = 2.2 * FogDensity / FogDistance;
    float top = SurfaceY + scale * 3.0;
    float enter = 0.0;
    float exit = min(limit, FogDistance * 1.5);
    if (top < 0.0) enter = dir.y < -1.0e-4 ? top / dir.y : exit;
    else if (dir.y > 1.0e-4) exit = min(exit, top / dir.y);
    int octaves = Quality >= 4 ? 4 : 3;
    float jitter = realm_ign(gl_FragCoord.xy);
    vec3 baseColor = mix(FogTint * 1.3, Glow * 0.6, Atmosphere == 2 ? 0.1 : 0.15);
    vec3 scatter = vec3(0.0);
    float transmit = 1.0;
    float span = exit - enter;
    float prev = enter;
    for (int i = 0; i < 16; i++) {
        if (i >= FogSteps || span <= 0.0) break;
        float k = (float(i) + jitter) / float(FogSteps);
        float t = enter + span * k * k;
        float next = enter + span * (float(i + 1) / float(FogSteps)) * (float(i + 1) / float(FogSteps));
        float dt = next - prev;
        prev = next;
        vec3 p = dir * t;
        float h = p.y - SurfaceY;
        float wisp = style_density(p + CameraPos, octaves);
        float density = rho * exp(-clamp(h / scale, -4.0, 30.0)) * ((Atmosphere == 1 ? 0.08 : 0.2) + 5.0 * wisp);
        // 絮团浓处更亮以显出体积，仍保持雾色；只有贴地一层被地面辉光微微映亮
        float ground = exp(-max(h, 0.0) / (Atmosphere == 1 ? 3.0 : 2.0));
        vec3 light = baseColor * (1.0 + 1.2 * wisp) + Glow * (Atmosphere == 1 ? 0.2 : 0.1) * ground;
        float absorb = 1.0 - exp(-density * dt);
        scatter += light * absorb * transmit;
        transmit *= 1.0 - absorb;
    }
    float rest = realm_height_fog(dir, limit, SurfaceY, rho, scale) - realm_height_fog(dir, max(exit, enter), SurfaceY, rho, scale);
    float far = 1.0 - exp(-max(rest, 0.0));
    scatter += baseColor * far * transmit;
    transmit *= 1.0 - far;
    // 与解析雾一致，保留至少 15% 的场景
    float cap = min(1.0, 0.85 / max(1.0 - transmit, 1.0e-4));
    return vec4(scatter * cap, 1.0 - (1.0 - transmit) * cap);
}

void main() {
    ivec2 texel = realm_coarse_texel(gl_FragCoord.xy, Downscale);
    vec2 uv = (vec2(texel) + 0.5) / vec2(textureSize(DepthSampler, 0));
    float depth = texelFetch(DepthSampler, texel, 0).r;
    bool sky = depth >= 1.0;
    vec3 pos = realm_view_pos(InvViewProj, uv, depth);
    vec3 dir = normalize(pos);
    float dist = sky ? REALM_SKY_DISTANCE : length(pos);

    vec3 scatter = vec3(0.0);
    float transmit = 1.0;
    if (Strength > 0.0) {
        float limit = dist;
        vec2 span = realm_ray_cylinder(dir, Bounds.xy, Bounds.z);
        if (span.y > 0.0 && length(Bounds.xy) < Bounds.z) limit = min(limit, span.y);
        if (FogSteps > 0) {
            vec4 volume = volumetric(dir, limit);
            scatter = volume.rgb * Strength;
            transmit = mix(1.0, volume.a, Strength);
        } else {
            float fog = min(1.0 - exp(-realm_height_fog(dir, limit, SurfaceY, 2.2 * FogDensity / FogDistance, fog_scale())), 0.85) * Strength;
            float ground = clamp(-dir.y * 2.0, 0.0, 1.0);
            float glowMix = Atmosphere == 1 ? 0.12 + 0.3 * ground : Atmosphere == 2 ? 0.08 : 0.12 + 0.1 * ground;
            scatter = mix(FogTint * 1.3, Glow * 0.6, glowMix) * fog;
            transmit = 1.0 - fog;
        }

        // 天空与云层等高空像素整体替换，避免云块与原版天空色差成斑
        float cover = sky ? 1.0 : smoothstep(30.0, 60.0, pos.y - SurfaceY);
        if (cover > 0.0) {
            int octaves = Quality >= 4 ? 4 : Quality >= 3 ? 3 : Quality >= 2 ? 2 : 1;
            cover *= Strength;
            scatter = mix(scatter, realm_sky(NoiseSampler, dir, Atmosphere, Tint, Glow, FogTint, Time, octaves), cover);
            transmit *= 1.0 - cover;
        }
    }

    // 雾穹：相机不在该领域内时，射线穿过已覆盖圆柱的区段
    for (int i = 0; i < 4; i++) {
        if (i >= RealmCount) break;
        vec4 shape = RealmShape[i];
        if (shape.w <= 0.0 || length(shape.xy) < shape.z) continue;
        vec2 hit = realm_ray_cylinder(dir, shape.xy, shape.z);
        float t0 = max(hit.x, 0.0);
        float t1 = min(hit.y, dist);
        if (hit.x > hit.y || t1 <= t0) continue;
        vec4 src = RealmSource[i];
        vec3 glow = RealmLook[i].rgb;
        vec3 mid = dir * (0.5 * (t0 + t1));
        float mh = mid.y - src.z;
        float covered = smoothstep(shape.w, shape.w - 4.0, length(mid.xz - src.xy));
        float tau = (t1 - t0) * 0.012 * exp(-max(mh, 0.0) / 12.0) * covered;
        float dome = min(1.0 - exp(-tau), 0.55);
        scatter = scatter * (1.0 - dome) + mix(glow * 0.35, glow * 0.7, exp(-max(mh, 0.0) / 8.0)) * dome;
        transmit *= 1.0 - dome;
    }
    if (Quality == 1) scatter += realm_fx(NoiseSampler, dir, dist, Time, Glow, RealmShape, RealmSource, RealmLook, RealmCount, Shock);
    fragColor = vec4(scatter, transmit);
}
