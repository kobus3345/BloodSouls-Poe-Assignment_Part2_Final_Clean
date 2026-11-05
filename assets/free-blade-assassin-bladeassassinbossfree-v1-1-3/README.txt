Blade Assassin Boss - Free  -  version 1.3.1

1 top-down boss with two forms (the normal form and its second form, Unmasked Daywalker), 8 directions each, plus 6 effects. A red-glow version of every normal-form animation is included.

- Blade Assassin: attack (blink strike), attack2 (phase rush), attack3 (blade slash), attack4 (whirlwind), death, hurt, idle, run, walk (415x361 px frames); attack 6, attack2 6, attack3 6, attack4 6, death 6, hurt 6, idle 4, run 6, walk 6 frames per direction
- Blade Assassin, red-glow version (characters/blade-assassin-enraged/): attack (blink strike), attack2 (phase rush), attack3 (blade slash), attack4 (whirlwind), death, hurt, idle, run, walk (415x361 px frames); attack 6, attack2 6, attack3 6, attack4 6, death 6, hurt 6, idle 4, run 6, walk 6 frames per direction

The red-glow version shares the normal form's frame size and feet pivot, so you can swap it in at any point (for example as a hit or warning flash).
Effects are in effects/<name>/: a horizontal sheet, single frames and a JSON with frame size and FPS.
Includes PNG artwork, animation metadata and Godot 4 SpriteFrames. Other engines can import the PNGs; their animation setup must be configured separately.

SECOND FORM (characters/blade-assassin-phase-two/)
  Blade Assassin, second form: Unmasked Daywalker. A new look for the second half of the fight, with its own attacks and effects.
  - attack (blink strike), attack2 (phase rush), attack3 (blade slash), attack4 (whirlwind), attack5 (Blood Mist), attack6 (Bone Lance), death, hurt, idle, run, walk (547x422 px frames, feet pivot 246, 404); attack 12, attack2 12, attack3 12, attack4 12, attack5 12, attack6 12, death 12, hurt 12, idle 12, run 6, walk 6 frames per direction
  Same folders as the normal form: sheets/, frames/, _sizes/128/, godot/ (SpriteFrames, animations <animation>_<direction>)
  and the JSON (frame size, feet pivot, fps, looping, attack names). Every animation has all 8 directions.
  To switch forms, play the second form at the same feet point as the normal form (both JSON files give the pivot).
  Its effects are in effects/<name>_phase_two/:
  blink_arrival_phase_two      the second form's blink arrival
  blood_mist_phase_two         a low pool of red mist that bursts upward in a ring, for Blood Mist
  bone_lance_impact_phase_two  a spray of black bone shards at the tip of the thrust, for Bone Lance
  phase_trail_phase_two        the second form's phase trail

SMOOTH CLIPS
  Every idle, walk, run, attack and hurt animation of the normal form (and its effects) also comes as <animation>_smooth: the same animation with twice the frames, played at twice the fps (same frame size, feet pivot and row order). Sheets, single frames, 128 px copies, colour variations and recolour masks included; Godot 4 SpriteFrames in godot/<name>_smooth_spriteframes.tres (animations <animation>_smooth_<direction>); the JSON lists them. Effects: <clip>_smooth sheets next to the originals, listed in each effect JSON.

STAGGER AND ENTRANCE (added in version 1.2.0)
  stagger   the boss reeling and catching its breath: an opening for the player after a big attack;
            loop it for as long as the opening lasts.
  entrance  the boss rising into the fight; play it once when the boss appears, then switch to idle.
  Same frame size, feet pivot, sheet layout, single frames and Godot SpriteFrames as the other
  animations (animation names stagger_<DIRECTION> and entrance_<DIRECTION>); colour variations and
  recolour masks included.
  - Blade Assassin: stagger (6 frames per direction, loops)

Injured idles: idle_hp50 and idle_hp25 (4 frames per direction, loops): the boss idling with battle damage at half and at a quarter health. Same frame size, feet pivot, sheet layout, single frames and Godot SpriteFrames as idle (animation names idle_hp50_<DIRECTION>, idle_hp25_<DIRECTION>); drawn in the normal colours.

COLOUR VARIATIONS (ColourVariations/, optional: delete the folder if you don't need it)
  ColourVariations/<variation>/<name>/  every animation of a character in another colour variation: the same
      sheets, frame size, feet pivot and Godot SpriteFrames as characters/<name>/ (sheets only; slice
      them like the main sheets). Previews: previews/<name>_colour_variations.png
  - Blade Assassin: Azure (azure/), Venom Green (venom/), Violet (violet/)
  - Blade Assassin (Second Form: Unmasked Daywalker): Azure (azure/), Venom Green (venom/), Violet (violet/)
  ColourVariations/recolour_masks/<name>/<clip>.png  material masks in the same layout as the sheets
      (red = main material, green = second material) for your own colours at runtime.
  ColourVariations/godot/recolour_mask.gdshader + recolour_masks.gd  Godot 4: give the AnimatedSprite2D a
      ShaderMaterial with the shader, attach the script, set masks_dir, then pick colour_main / colour_second.

MORE IN THIS STYLE
  Daywalkers: https://sovdigital.itch.io/daywalkers
  Bosses Set: https://sovdigital.itch.io/bosses-set
  Complete Edition (every pack, boss and effect in one download): https://sovdigital.itch.io/complete-edition
  Everything by Sova Digital: https://sovdigital.itch.io/

Anything added to this pack later is free if you already own it.
