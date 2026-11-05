# Attach to an AnimatedSprite2D that uses a character's SpriteFrames and a ShaderMaterial with recolour_mask.gdshader.
# Animations are named <clip>_<DIRECTION>; this loads ColourVariations/recolour_masks/<name>/<clip>.png for the current clip.
extends AnimatedSprite2D

@export_dir var masks_dir: String = ""   # e.g. res://ColourVariations/recolour_masks/footsoldier

var _cache := {}

func _ready() -> void:
	animation_changed.connect(_apply_mask)
	_apply_mask()

func _apply_mask() -> void:
	var mat := material as ShaderMaterial
	if mat == null or masks_dir == "":
		return
	var clip := String(animation)
	var cut := clip.rfind("_")
	if cut > 0:
		clip = clip.substr(0, cut)
	if not _cache.has(clip):
		var path := masks_dir.path_join(clip + ".png")
		_cache[clip] = load(path) if ResourceLoader.exists(path) else null
	mat.set_shader_parameter("mask_tex", _cache[clip])
