// 领域噪声：ALU 哈希、值噪声、Voronoi 与基于噪声纹理的 3D 值噪声，不声明 uniform。

float realm_hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float realm_hash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

vec2 realm_hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float realm_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(realm_hash(i), realm_hash(i + vec2(1.0, 0.0)), u.x),
               mix(realm_hash(i + vec2(0.0, 1.0)), realm_hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float realm_fbm(vec2 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) break;
        sum += amp * realm_noise(p);
        p = mat2(1.6, 1.2, -1.2, 1.6) * p + vec2(17.3, 9.1);
        amp *= 0.5;
    }
    return sum;
}

// 返回 (F1, F2)：到最近与次近特征点的距离。
vec2 realm_voronoi(vec2 p) {
    vec2 cell = floor(p);
    vec2 f = fract(p);
    float f1 = 8.0;
    float f2 = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(float(x), float(y));
            float d = length(g + realm_hash2(cell + g) - f);
            if (d < f1) {
                f2 = f1;
                f1 = d;
            } else if (d < f2) {
                f2 = d;
            }
        }
    }
    return vec2(f1, f2);
}

// 基于噪声纹理的 2D 值噪声与 fbm：一次采样代替四次哈希，供后处理大面积使用。
float realm_tnoise(sampler2D noise, vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return textureLod(noise, (i + f + 0.5) / 256.0, 0.0).g;
}

float realm_tfbm(sampler2D noise, vec2 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) break;
        sum += amp * realm_tnoise(noise, p);
        p = mat2(1.6, 1.2, -1.2, 1.6) * p + vec2(17.3, 9.1);
        amp *= 0.5;
    }
    return sum;
}

// 噪声纹理的 R 通道是 G 偏移 (37, 17) 的值，单次采样即可在 z 方向插值。
float realm_noise3(sampler2D noise, vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = i.xy + vec2(37.0, 17.0) * i.z + f.xy;
    vec2 rg = textureLod(noise, (uv + 0.5) / 256.0, 0.0).yx;
    return mix(rg.x, rg.y, f.z);
}

float realm_fbm3(sampler2D noise, vec3 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        if (i >= octaves) break;
        sum += amp * realm_noise3(noise, p);
        p = p * 2.03 + vec3(11.7, 5.3, 3.1);
        amp *= 0.5;
    }
    return sum;
}
