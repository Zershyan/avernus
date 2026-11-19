#version 150

#moj_import <fog.glsl>
#moj_import <avernus:realm_noise.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;

in float vertexDistance;
in vec3 viewPos;
in float shade;
in vec4 glow;
in vec2 texCoord0;
in vec3 bary;
flat in ivec2 state;

out vec4 fragColor;

// 到镜片边缘的距离：重心坐标最小分量，跳过多边形三角化产生的内部对角线。
float edgeDistance() {
    float y = (state.y & 512) != 0 ? 1.0 : bary.y;
    float z = (state.y & 1024) != 0 ? 1.0 : bary.z;
    return min(bary.x, min(y, z));
}

// 裂纹距离：叠加随镜面位置变化的抖动，让裂纹曲折而不是笔直；相邻镜片取样一致，线条连续。
float crackDistance() {
    float wobble = realm_noise(texCoord0 * 14.0) - 0.5 + (realm_noise(texCoord0 * 47.0) - 0.5) * 0.45;
    return abs(edgeDistance() + wobble * 0.07);
}

// 按屏幕导数保持线宽恒定。
float line(float distance, float width) {
    return 1.0 - smoothstep(0.0, fwidth(distance) * width, distance);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    float crack = float(state.x) / 255.0;
    float outline = float(state.y & 255) / 255.0;
    float seconds = GameTime * 1200.0;
    float edge = line(edgeDistance(), 1.6) * outline * (0.75 + 0.25 * sin(seconds * 4.0));

    if ((state.y & 256) != 0) {
        fragColor = vec4(mix(color.rgb, glow.rgb, 0.6) * (0.2 + edge * 0.6), glow.a * color.a) * ColorModulator;
        return;
    }

    vec3 normal = normalize(cross(dFdx(viewPos), dFdy(viewPos)));
    float fresnel = pow(1.0 - abs(dot(normal, normalize(-viewPos))), 4.0);
    float sweep = fract((texCoord0.x + texCoord0.y) * 0.5 - seconds * 0.35);
    float sheen = smoothstep(0.0, 0.06, sweep) * (1.0 - smoothstep(0.06, 0.14, sweep));
    // 蓄势前半：裂纹沿真实镜片分割线从中心蔓延；后半：裂缝变宽透光，光渗进镜面，临碎前整面发亮
    float spreadPhase = clamp(crack * 2.0, 0.0, 1.0);
    float lightPhase = clamp(crack * 2.0 - 1.0, 0.0, 1.0);
    float reach = 1.0 - smoothstep(spreadPhase - 0.1, spreadPhase, length(texCoord0 - 0.5) * 1.6);
    float cracks = line(crackDistance(), 1.2 + lightPhase * 2.5) * reach * step(0.001, crack);
    // 主裂纹两侧分出的细碎支纹：噪声等值线，只在主裂纹附近出现
    float ridge = abs(realm_noise(texCoord0 * 9.0 + 3.7) - 0.5);
    float branch = (1.0 - smoothstep(0.0, fwidth(ridge) * 1.1, ridge)) * exp(-crackDistance() * 14.0) * reach * step(0.001, crack);
    cracks = max(cracks, branch * 0.7);
    float bleed = exp(-crackDistance() * 18.0) * reach * lightPhase * (0.8 + 0.2 * sin(seconds * 30.0));
    float burst = lightPhase * lightPhase * lightPhase * lightPhase;
    vec3 crackColor = mix(vec3(0.85), glow.rgb * 2.2, lightPhase);

    vec3 rgb = mix(color.rgb * shade, glow.rgb, burst * 0.7) + glow.rgb * (fresnel * 0.3 + sheen * 0.3 + edge * 0.9 + bleed * 1.2);
    rgb = mix(rgb, crackColor, cracks);
    fragColor = linear_fog(vec4(rgb, color.a * glow.a) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
