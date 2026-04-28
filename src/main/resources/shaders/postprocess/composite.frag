#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uScene;
uniform sampler2D uBloom;
uniform float uBloomIntensity;

void main() {
    vec4 sceneColor = texture(uScene, vUv);
    vec3 bloomColor = texture(uBloom, vUv).rgb * uBloomIntensity;
    FragColor = vec4(sceneColor.rgb + bloomColor, sceneColor.a);
}
