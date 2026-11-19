// 领域空间几何：坐标全部相对相机，射线原点为相机。

vec3 realm_view_pos(mat4 invViewProj, vec2 uv, float depth) {
    vec4 world = invViewProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return world.xyz / world.w;
}

// 天空像素视作的距离。
const float REALM_SKY_DISTANCE = 400.0;

// 附近领域打包：RealmLook 列 w = 境相形态 + 4·波面运动方向（扩张 1，收缩 -1）。
float realm_flow(vec4 look) {
    return look.w > 3.5 ? 1.0 : look.w < -0.5 ? -1.0 : 0.0;
}

int realm_atmosphere(vec4 look) {
    return int(look.w - 4.0 * realm_flow(look) + 0.5);
}

// 降分辨率通道的采样点：每个 n×n 块的左下像素。
ivec2 realm_coarse_texel(vec2 fragCoord, int downscale) {
    return ivec2(fragCoord) * downscale;
}

// 射线与竖直无限圆柱的交点参数 (t0, t1)，无交点时 t0 > t1。
vec2 realm_ray_cylinder(vec3 dir, vec2 center, float radius) {
    vec2 d = dir.xz;
    float a = dot(d, d);
    float b = -dot(center, d);
    float c = dot(center, center) - radius * radius;
    float h = b * b - a * c;
    if (h < 0.0 || a < 1.0e-6) return vec2(1.0, -1.0);
    h = sqrt(h);
    return vec2((-b - h) / a, (-b + h) / a);
}

// 射线（单位方向）与球的交点参数 (t0, t1)，无交点时 t0 > t1。
vec2 realm_ray_sphere(vec3 dir, vec3 center, float radius) {
    float b = dot(center, dir);
    float h = b * b - dot(center, center) + radius * radius;
    if (h < 0.0) return vec2(1.0, -1.0);
    h = sqrt(h);
    return vec2(b - h, b + h);
}

// 圆柱内解析高度雾的光学厚度：密度 rho0·exp(-(y - surfaceY)/h)，沿射线积分到 dist。
float realm_height_fog(vec3 dir, float dist, float surfaceY, float rho0, float h) {
    float k = dir.y;
    float base = rho0 * exp(clamp(surfaceY / h, -30.0, 8.0));
    float path = abs(k) < 1.0e-3 ? dist : h * (1.0 - exp(clamp(-dist * k / h, -30.0, 30.0))) / k;
    return clamp(base * path, 0.0, 8.0);
}
