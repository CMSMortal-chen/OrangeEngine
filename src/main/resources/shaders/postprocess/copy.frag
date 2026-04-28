#version 330 core

in vec2 vUv;
out vec4 FragColor;

uniform sampler2D uScene;

void main() {
    FragColor = texture(uScene, vUv);
}
