#version 150

#moj_import <avernus:realm_noise.glsl>
#moj_import <avernus:realm_space.glsl>
#moj_import <avernus:realm_sky.glsl>
#moj_import <avernus:realm_fx.glsl>

// 降分辨率氛围通道：输出 (内散射 rgb, 透射率 a)，合成为 color·a + rgb。
// 含当前领域的雾（低档解析高度雾，中档起贴地薄霭加步进的立体雾团）与程序化天空（已乘 Strength）、境外看领域时的雾穹；低档无泛光，光幕也并入这里。

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

// 雾团的最高处（离地格数）。
float volume_top() {
    return Atmosphere == 1 ? 34.0 : Atmosphere == 2 ? 18.0 : 26.0;
}

// 雾团噪声的各向频率（格⁻¹）：首倍频一格即一团的尺度，竖直更密则团更扁。
vec3 volume_frequency() {
    return Atmosphere == 1 ? vec3(0.05, 0.04, 0.05) : Atmosphere == 2 ? vec3(0.028, 0.07, 0.028) : vec3(0.04, 0.055, 0.04);
}

// 雾团漂移（格/秒）：炼狱烟团上涌，寂静冰雾横移，幽冥缓慢游荡。
vec3 volume_wind() {
    return Atmosphere == 1 ? vec3(0.4, 1.1, 0.2) : Atmosphere == 2 ? vec3(0.9, 0.0, 0.3) : vec3(-0.5, 0.15, 0.6);
}

// 雾团浓度 0..1：贴地处约三成体积成团，越高越难成团，团顶因此收圆，团间留出能看穿的空隙。
// footprint 为一步跨过的首倍频噪声格数，团边随之放宽，免得稀疏步进把团边切成一层层纹路。
float volume_density(vec3 world, float h, int octaves, float footprint) {
    float top = volume_top();
    if (h < -2.0 || h > top) return 0.0;
    float shape = realm_fbm3_lod(NoiseSampler, (world - volume_wind() * Time) * volume_frequency(), octaves, footprint) / (1.0 - exp2(-float(octaves)));
    float base = Atmosphere == 1 ? 0.58 : Atmosphere == 2 ? 0.54 : 0.56;
    float threshold = base + 0.24 * pow(clamp(h / top, 0.0, 1.0), 1.5);
    float soft = 0.06 + 0.35 * footprint;
    return smoothstep(threshold - soft * 0.5, threshold + soft * 0.5, shape) * smoothstep(-2.0, 1.0, h) * smoothstep(top, top * 0.85, h);
}

// 把一段均匀介质（光学厚度 tau、出射光 light）叠到 (散射 rgb, 透射率 a) 上。
void accumulate(inout vec4 acc, vec3 light, float tau) {
    float absorb = 1.0 - exp(-tau);
    acc.rgb += light * absorb * acc.a;
    acc.a *= 1.0 - absorb;
}

// 体积雾：贴地薄霭解析积分，成团的雾只在其高度段内步进，步点向相机加密；步进段以外的雾团按平均浓度解析补足。
// 返回 (散射 rgb, 透射率 a)，未乘 Strength。
vec4 volumetric(vec3 dir, float limit) {
    float scale = fog_scale();
    float rho = 2.2 * FogDensity / FogDistance;
    float haze = rho * 0.35;
    float mass = rho * 3.5;
    float top = volume_top();
    float bottom = SurfaceY - 2.0;
    float enter = 0.0;
    float exit = min(limit, FogDistance * (Quality >= 4 ? 2.5 : 1.5));
    if (abs(dir.y) > 1.0e-4) {
        float a = bottom / dir.y, b = (SurfaceY + top) / dir.y;
        enter = max(enter, min(a, b));
        exit = min(exit, max(a, b));
    } else if (bottom > 0.0 || SurfaceY + top < 0.0) {
        exit = enter;
    }
    enter = min(enter, limit);
    int octaves = Quality >= 4 ? 4 : 3;
    vec3 baseColor = mix(FogTint * 1.3, Glow * 0.6, Atmosphere == 2 ? 0.1 : 0.15);
    vec3 hazeColor = baseColor * 0.9;
    float underglow = Atmosphere == 1 ? 0.35 : 0.12;
    float rayFrequency = length(volume_frequency() * dir);
    float lift = top * 0.25;
    vec4 acc = vec4(0.0, 0.0, 0.0, 1.0);
    float hazeDepth = realm_height_fog(dir, max(enter, 0.0), SurfaceY, haze, scale);
    accumulate(acc, hazeColor, hazeDepth);
    float span = max(exit - enter, 0.0);
    float prev = enter;
    for (int i = 0; i < 32; i++) {
        if (i >= FogSteps || span <= 0.0) break;
        // 步点取段中点而不逐像素抖动：降分辨率下抖动无从平均，会在暗处显成满屏颗粒
        float k = (float(i) + 0.5) / float(FogSteps);
        float t = enter + span * k * k;
        float next = enter + span * (float(i + 1) / float(FogSteps)) * (float(i + 1) / float(FogSteps));
        float dt = next - prev;
        prev = next;
        vec3 p = dir * t;
        vec3 world = p + CameraPos;
        float h = p.y - SurfaceY;
        float footprint = dt * rayFrequency;
        // 身边几格的雾团减淡，置身团中也看得清脚下
        float cloud = volume_density(world, h, octaves, footprint) * mix(0.3, 1.0, smoothstep(2.0, 12.0, t));
        float segmentHaze = realm_height_fog(dir, next, SurfaceY, haze, scale) - hazeDepth;
        hazeDepth += segmentHaze;
        float cloudDepth = mass * cloud * dt;
        vec3 light = hazeColor;
        if (cloud > 0.01) {
            // 朝上取一次浓度作自遮挡：团顶受光发亮，团底与团心偏暗，雾团因此显出高度与厚度；贴地一侧被地面辉光映亮
            float hn = clamp(h / top, 0.0, 1.0);
            float sun = exp(-1.5 * volume_density(world + vec3(0.0, lift, 0.0), h + lift, 2, footprint));
            vec3 lit = baseColor * (0.7 + (0.5 + 0.7 * hn) * sun) + Glow * underglow * (1.0 - hn) * (1.0 - hn);
            light = (hazeColor * segmentHaze + lit * cloudDepth) / max(segmentHaze + cloudDepth, 1.0e-6);
        }
        accumulate(acc, light, segmentHaze + cloudDepth);
    }
    float meanScale = top * 0.25;
    float rest = realm_height_fog(dir, limit, SurfaceY, haze, scale) - hazeDepth
            + realm_height_fog(dir, limit, SurfaceY, mass * 0.2, meanScale) - realm_height_fog(dir, max(exit, enter), SurfaceY, mass * 0.2, meanScale);
    accumulate(acc, baseColor, max(rest, 0.0));
    // 与解析雾一致，保留至少 15% 的场景
    float cap = min(1.0, 0.85 / max(1.0 - acc.a, 1.0e-4));
    return vec4(acc.rgb * cap, 1.0 - (1.0 - acc.a) * cap);
}

void main() {
    ivec2 texel = realm_coarse_texel(gl_FragCoord.xy, Downscale);
    vec2 uv = (vec2(texel) + 0.5) / vec2(textureSize(DepthSampler, 0));
    float depth = texelFetch(DepthSampler, texel, 0).r;
    bool sky = depth >= 1.0;
    vec3 pos = realm_view_pos(InvViewProj, uv, depth);
    vec3 dir = normalize(pos);
    float dist = sky ? REALM_SKY_DISTANCE : length(pos);
    float pixel = length(fwidth(dir));

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

        // 天空与云层等高空像素整体替换为程序化天空，避免云块与原版天空色差成斑；极致档天空垫在雾后，雾团的轮廓得以映在天上
        float cover = sky ? 1.0 : smoothstep(30.0, 60.0, pos.y - SurfaceY);
        if (cover > 0.0) {
            int octaves = Quality >= 4 ? 4 : Quality >= 3 ? 3 : Quality >= 2 ? 2 : 1;
            cover *= Strength;
            vec3 heaven = realm_sky(NoiseSampler, dir, Atmosphere, Tint, Glow, FogTint, Time, octaves, pixel);
            scatter = Quality >= 4 ? scatter + heaven * transmit * cover : mix(scatter, heaven, cover);
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
