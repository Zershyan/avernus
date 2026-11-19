#version 150

// 泛光上采样：9 点帐篷滤波，加性混合叠到上一级。

uniform sampler2D SourceSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 px = 1.0 / vec2(textureSize(SourceSampler, 0));
    vec3 color = texture(SourceSampler, texCoord).rgb * 4.0;
    color += (texture(SourceSampler, texCoord + vec2(px.x, 0.0)).rgb + texture(SourceSampler, texCoord - vec2(px.x, 0.0)).rgb
            + texture(SourceSampler, texCoord + vec2(0.0, px.y)).rgb + texture(SourceSampler, texCoord - vec2(0.0, px.y)).rgb) * 2.0;
    color += texture(SourceSampler, texCoord + px).rgb + texture(SourceSampler, texCoord - px).rgb
            + texture(SourceSampler, texCoord + vec2(px.x, -px.y)).rgb + texture(SourceSampler, texCoord + vec2(-px.x, px.y)).rgb;
    fragColor = vec4(color / 16.0, 1.0);
}
