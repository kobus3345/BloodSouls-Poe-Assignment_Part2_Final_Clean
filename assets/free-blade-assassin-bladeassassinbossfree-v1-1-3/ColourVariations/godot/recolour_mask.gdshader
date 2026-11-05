// Recolour mask shader (Godot 4). Keeps the sprite's own shading and replaces the hue of the masked materials.
// mask_tex: the matching sheet from ColourVariations/recolour_masks/<name>/<clip>.png (R = main material, G = second).
// recolour_masks.gd swaps mask_tex when the AnimatedSprite2D changes animation.
shader_type canvas_item;

uniform sampler2D mask_tex : filter_linear;
uniform vec4 colour_main : source_color = vec4(0.70, 0.16, 0.14, 1.0);
uniform vec4 colour_second : source_color = vec4(0.12, 0.12, 0.13, 1.0);
uniform float strength_main : hint_range(0.0, 1.0) = 1.0;
uniform float strength_second : hint_range(0.0, 1.0) = 0.0;
uniform float brightness : hint_range(0.2, 2.0) = 1.0;

vec3 tint(vec3 base, vec3 target) {
	const vec3 W = vec3(0.299, 0.587, 0.114);
	float lum = dot(base, W);
	float tl = max(dot(target, W), 0.001);
	return clamp(target * (lum / tl) * brightness, 0.0, 1.0);
}

void fragment() {
	vec4 base = COLOR;
	vec2 m = texture(mask_tex, UV).rg;
	vec3 rgb = mix(base.rgb, tint(base.rgb, colour_main.rgb), m.r * strength_main);
	rgb = mix(rgb, tint(base.rgb, colour_second.rgb), m.g * strength_second);
	COLOR = vec4(rgb, base.a);
}
