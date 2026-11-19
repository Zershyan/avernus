#version 150

// 泛光下采样：首级读全分辨率画面，用 4 次双线性采样做盒式滤波并提取亮度（发光遮罩部分全量 + 境内软阈值高光）；
// 之后各级用 13 点滤波。

uniform sampler2D SourceSampler;

uniform int Prefilter;
uniform float Strength;

in vec2 texCoord;

out vec4 fragColor;

const vec3 LUMA = vec3(0.299, 0.587, 0.114);

vec3 bright(vec4 c) {
    float luma = dot(c.rgb, LUMA);
    float soft = clamp(luma - 0.6, 0.0, 0.3);
    float knee = max(soft * soft / 1.2, luma - 0.75) / max(luma, 1.0e-4);
    return c.rgb * (c.a + max(knee, 0.0) * Strength);
}

vec3 tap(vec2 uv) {
    return texture(SourceSampler, uv).rgb;
}

void main() {
    vec2 px = 1.0 / vec2(textureSize(SourceSampler, 0));
    if (Prefilter == 1) {
        vec3 sum = bright(texture(SourceSampler, texCoord + px * vec2(-1.0, -1.0))) + bright(texture(SourceSampler, texCoord + px * vec2(1.0, -1.0)))
                + bright(texture(SourceSampler, texCoord + px * vec2(-1.0, 1.0))) + bright(texture(SourceSampler, texCoord + px * vec2(1.0, 1.0)));
        fragColor = vec4(sum * 0.25, 1.0);
        return;
    }
    vec3 a = tap(texCoord + px * vec2(-2.0, 2.0));
    vec3 b = tap(texCoord + px * vec2(0.0, 2.0));
    vec3 c = tap(texCoord + px * vec2(2.0, 2.0));
    vec3 d = tap(texCoord + px * vec2(-2.0, 0.0));
    vec3 e = tap(texCoord);
    vec3 f = tap(texCoord + px * vec2(2.0, 0.0));
    vec3 g = tap(texCoord + px * vec2(-2.0, -2.0));
    vec3 h = tap(texCoord + px * vec2(0.0, -2.0));
    vec3 i = tap(texCoord + px * vec2(2.0, -2.0));
    vec3 j = tap(texCoord + px * vec2(-1.0, 1.0));
    vec3 k = tap(texCoord + px * vec2(1.0, 1.0));
    vec3 l = tap(texCoord + px * vec2(-1.0, -1.0));
    vec3 m = tap(texCoord + px * vec2(1.0, -1.0));
    vec3 color = e * 0.125 + (a + c + g + i) * 0.03125 + (b + d + f + h) * 0.0625 + (j + k + l + m) * 0.125;
    fragColor = vec4(color, 1.0);
}
