#version 150

#moj_import <fog.glsl>
#moj_import <avernus:realm.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec3 terrainPos;
in vec3 normal;
in vec4 lightColor;

out vec4 fragColor;

void main() {
    if (realm_discard(terrainPos, normal, vertexColor.a)) {
        discard;
    }
    vec4 color = texture(Sampler0, texCoord0) * vec4(vertexColor.rgb, RealmMeshMode == 2 ? 1.0 : vertexColor.a) * lightColor * ColorModulator;
    if (color.a < 0.5) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
