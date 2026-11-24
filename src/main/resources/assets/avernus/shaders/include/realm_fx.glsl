// 光幕：波面与界壁的絮雾、碎裂冲击壳的自发光（加性）。依赖 realm_noise.glsl 与 realm_space.glsl。

const float REALM_SHOCK_SECONDS = 1.5;

// 絮雾层：圆柱壳由外向内取两层，每层间隔 REALM_WISP_STEP 格，掠射时路径变长更亮，远处淡出以融入边界雾。
// 弧长坐标在正西（atan 的 ±π 处）首尾相接，接缝前 REALM_WISP_SEAM 格内与绕行一周后的取值交叉过渡，免得显出一条竖线。
const float REALM_WISP_SEAM = 24.0;
const float REALM_WISP_STEP = 2.5;

vec3 realm_wisp_tint(vec3 glow, float height) {
    return mix(glow * 1.25, glow * 0.45, clamp(height / 30.0, 0.0, 1.0));
}

// 絮雾密度：along 为沿弧长坐标，height 为离地高度，flow 为沿弧方向的漂移方向。
float realm_wisp(sampler2D noise, float along, float height, int atmosphere, float time, float flow) {
    if (atmosphere == 1) {
        // 炼狱：向上翻涌的烟火
        float n = realm_tfbm(noise, vec2(along * 0.09 + time * 0.2 * flow, height * 0.08 - time * 1.2), 2);
        return smoothstep(0.3, 0.8, n + 0.15 - height * 0.01);
    }
    if (atmosphere == 2) {
        // 寂静：缓慢横移的冰雾
        return smoothstep(0.35, 0.9, realm_tfbm(noise, vec2(along * 0.04 + time * 0.12 * flow, height * 0.035 - time * 0.1), 2));
    }
    // 幽冥：缓缓上浮的魂雾
    return smoothstep(0.35, 0.85, realm_tfbm(noise, vec2(along * 0.07 + time * 0.15 * flow, height * 0.06 - time * 0.35), 2));
}

// clip.z < 0 表示不裁剪，否则只保留 clip.xy 为圆心、clip.z 为半径内的部分。
vec3 realm_wisp_shell(sampler2D noise, vec3 dir, float dist, vec2 center, float radius, vec3 clip, float baseY, float falloff, float top,
                      vec3 glow, int atmosphere, float time, float flow) {
    vec3 sum = vec3(0.0);
    for (int layer = 0; layer < 2; layer++) {
        float r = radius - float(layer) * REALM_WISP_STEP;
        vec2 hit = realm_ray_cylinder(dir, center, r);
        for (int k = 0; k < 2; k++) {
            float t = k == 0 ? hit.x : hit.y;
            if (hit.x > hit.y || t <= 0.0 || t >= dist) continue;
            vec3 p = dir * t;
            if (clip.z >= 0.0 && length(p.xz - clip.xy) > clip.z) continue;
            float h = p.y - baseY;
            vec2 radial = p.xz - center;
            float graze = min(1.0 / max(abs(dot(normalize(radial), normalize(dir.xz))), 0.25), 4.0);
            float body = exp(-max(h, 0.0) / falloff) * smoothstep(-1.0, 1.0, h) * smoothstep(top, top * 0.6, h);
            float fade = mix(1.0, 0.35, smoothstep(40.0, 200.0, t));
            float along = atan(radial.y, radial.x) * r;
            float wisp = realm_wisp(noise, along, h, atmosphere, time, flow);
            float seam = smoothstep(3.14159265 * r - REALM_WISP_SEAM, 3.14159265 * r, along);
            if (seam > 0.0) wisp = mix(wisp, realm_wisp(noise, along - 6.28318531 * r, h, atmosphere, time, flow), seam);
            sum += realm_wisp_tint(glow, h) * wisp * body * graze * fade;
        }
    }
    return sum * 0.5;
}

vec3 realm_fx(sampler2D noise, vec3 dir, float dist, float time, vec3 glow, mat4 shapes, mat4 sources, mat4 looks, int count, vec4 shock) {
    vec3 fx = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        if (i >= count) break;
        vec4 shape = shapes[i];
        if (shape.w <= 0.0) continue;
        vec4 src = sources[i];
        vec4 look = looks[i];
        float flow = realm_flow(look);
        int atmosphere = realm_atmosphere(look);
        // 波面絮雾：只在波面移动时出现
        if (flow != 0.0) {
            fx += realm_wisp_shell(noise, dir, dist, src.xy, shape.w, shape.xyz, src.z, 9.0, 32.0, look.rgb, atmosphere, time, flow) * 0.45;
        }
        // 界壁絮雾：比波面淡，贴着边界雾穹
        if (src.w > 0.0) {
            fx += realm_wisp_shell(noise, dir, dist, shape.xy, shape.z, vec3(0.0, 0.0, -1.0), src.z, 22.0, 60.0, look.rgb, atmosphere, time * 0.5, 1.0)
                    * src.w * 0.18;
        }
    }

    // 冲击壳：碎裂后迅速扩张的球壳，掠射边缘提亮
    if (shock.w >= 0.0 && shock.w < REALM_SHOCK_SECONDS) {
        float radius = 30.0 * (1.0 - exp(-3.0 * shock.w));
        vec2 hit = realm_ray_sphere(dir, shock.xyz, radius);
        if (hit.x <= hit.y && (hit.x > 0.0 && hit.x < dist || hit.y > 0.0 && hit.y < dist)) {
            float rim = pow(clamp(length(shock.xyz - dir * dot(shock.xyz, dir)) / radius, 0.0, 1.0), 6.0);
            fx += glow * rim * exp(-2.5 * shock.w) * (1.0 - shock.w / REALM_SHOCK_SECONDS) * 1.5;
        }
    }
    return fx;
}
