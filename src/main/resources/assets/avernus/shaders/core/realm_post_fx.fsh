#version 150

#moj_import <avernus:realm_noise.glsl>
#moj_import <avernus:realm_space.glsl>
#moj_import <avernus:realm_fx.glsl>

// 降分辨率光幕通道（中档及以上）：a 存视距，供上采样判断轮廓。

uniform sampler2D DepthSampler;
uniform sampler2D NoiseSampler;

uniform mat4 InvViewProj;
uniform int Downscale;
uniform float Time;
uniform vec3 Glow;
uniform mat4 RealmShape;
uniform mat4 RealmSource;
uniform mat4 RealmLook;
uniform int RealmCount;
uniform vec4 Shock;

out vec4 fragColor;

void main() {
    ivec2 texel = realm_coarse_texel(gl_FragCoord.xy, Downscale);
    vec2 uv = (vec2(texel) + 0.5) / vec2(textureSize(DepthSampler, 0));
    float depth = texelFetch(DepthSampler, texel, 0).r;
    vec3 pos = realm_view_pos(InvViewProj, uv, depth);
    vec3 dir = normalize(pos);
    float dist = depth >= 1.0 ? REALM_SKY_DISTANCE : length(pos);

    vec3 fx = realm_fx(NoiseSampler, dir, dist, Time, Glow, RealmShape, RealmSource, RealmLook, RealmCount, Shock);
    fragColor = vec4(fx, dist);
}
