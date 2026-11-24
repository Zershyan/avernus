// 三境相程序化天空，依赖 realm_noise.glsl；颜色全部来自外观数据。

// 稀疏星点：方向按网格取格，格内随机一点为星；pixel 为单个像素对应的方向变化量，须在统一控制流中由调用方求得。
float realm_stars(vec3 dir, float density, float size, float time, float twinkle, float pixel) {
    vec3 p = dir * density;
    vec3 cell = floor(p);
    float h = realm_hash(cell);
    if (h < 0.82) return 0.0;
    vec3 star = cell + 0.5 + (vec3(realm_hash(cell + 7.1), realm_hash(cell + 3.7), realm_hash(cell + 1.3)) - 0.5) * 0.6;
    float d = length(p - star);
    float flicker = mix(1.0, 0.5 + 0.5 * sin(time * (2.0 + h * 5.0) + h * 40.0), twinkle);
    // 星点半径不小于一个像素，按面积比例降亮度，避免亚像素星点逐帧闪烁
    float radius = max(size, pixel * density * 1.5);
    float energy = size * size / (radius * radius);
    return smoothstep(radius, 0.0, d) * energy * (h - 0.82) * 5.5 * flicker;
}

// 极光帷幕：沿水平投影的扭曲光带，竖向条纹缓慢流动。
float realm_aurora(sampler2D noise, vec3 dir, float height, float time, int octaves) {
    float y = max(dir.y, 0.0) + 0.12;
    vec2 uv = dir.xz / y;
    float warp = realm_tfbm(noise, vec2(uv.x * 0.18 + time * 0.03, uv.y * 0.12 - time * 0.02), octaves);
    float d = uv.y * 0.22 + warp * 1.6 - height;
    float band = exp(-d * d * 18.0);
    float streak = 0.55 + 0.45 * sin(uv.x * 3.0 + warp * 9.0 + time * 0.4);
    return band * streak * smoothstep(0.0, 0.18, dir.y) * smoothstep(0.95, 0.55, dir.y);
}

vec3 realm_sky(sampler2D noise, vec3 dir, int atmosphere, vec3 tint, vec3 glow, vec3 fog, float time, int octaves, float pixel) {
    float up = clamp(dir.y, -1.0, 1.0);
    float horizon = exp(-abs(up) * 7.0);
    if (atmosphere == 1) {
        // 炼狱：翻滚烟云，下缘被火光映亮，地平线熔光与上升火星。
        vec2 uv = dir.xz / (max(up, 0.0) + 0.25);
        float smoke = realm_tfbm(noise, uv * 0.55 + vec2(time * 0.02, -time * 0.05), octaves + 1);
        float billow = abs(smoke * 2.0 - 1.0);
        vec3 color = mix(fog * 1.6, fog * 0.45, smoothstep(-0.05, 0.6, up));
        color += mix(glow * 0.9, fog * 2.6, smoothstep(0.0, 0.5, up)) * (1.0 - billow) * 0.6;
        color += glow * horizon * 0.9 + tint * pow(1.0 - max(up, 0.0), 6.0) * smoke * 0.25;
        vec3 rising = vec3(dir.x, dir.y - time * 0.015, dir.z);
        color += glow * realm_stars(rising, 70.0, 0.22, time, 1.0, pixel) * smoothstep(0.55, 0.0, up) * 1.6;
        return color;
    }
    if (atmosphere == 2) {
        // 寂静：深蓝星空、银河带与低空冷极光。
        vec3 color = mix(mix(fog, tint, 0.18) * 1.3, fog * 0.35, smoothstep(-0.05, 0.75, up));
        vec3 axis = normalize(vec3(0.35, 0.25, 1.0));
        float galaxy = exp(-pow(dot(dir, axis), 2.0) * 22.0);
        // 球面上直接取 3D 噪声：按方位角取样会在正北首尾相接处留下一道竖缝
        color += mix(tint, glow, 0.5) * galaxy * (0.25 + 0.5 * realm_fbm3(noise, dir * 4.0, octaves)) * 0.45;
        color += glow * realm_stars(dir, 140.0, 0.18, time, 1.0, pixel) * (0.6 + galaxy) * smoothstep(-0.02, 0.2, up);
        color += mix(glow, tint, 0.4) * realm_aurora(noise, dir, 0.55, time * 0.6, octaves) * 0.55;
        color += tint * horizon * 0.12;
        return color;
    }
    // 幽冥：墨绿天幕，流动鬼火极光与稀疏暗星。
    vec3 color = mix(mix(fog, tint, 0.25) * 1.2, fog * 0.4, smoothstep(-0.05, 0.7, up));
    float a = realm_aurora(noise, dir, 0.35, time, octaves);
    float b = octaves > 1 ? realm_aurora(noise, dir.zyx * vec3(1.0, 1.0, -1.0), 0.9, time * 0.8 + 13.0, octaves) : 0.0;
    color += mix(glow, tint, smoothstep(0.1, 0.6, up)) * (a + b * 0.6) * 0.9;
    color += tint * realm_stars(dir, 110.0, 0.2, time, 0.6, pixel) * 0.5 * smoothstep(0.0, 0.25, up);
    color += glow * horizon * 0.08;
    return color;
}
