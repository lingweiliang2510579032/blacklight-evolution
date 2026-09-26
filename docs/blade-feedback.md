# Blade presentation

The existing connected double-ended blade keeps its elbow mount and orientation. Its main edge beyond the forearm is extended by 22%, the reverse edge by 10%, and its cross-section by 16%. Mesh normals and both sampled trail endpoints use the same sizing transform. Damage values are unchanged. Subsequent reach progression and environment interaction are described in [attack-environment-and-reach.md](attack-environment-and-reach.md).

Blade swings now use their own layered ribbons: a muted red wake, pale cutting body and a short bright edge. Both ends follow the rendered elbow joint. Trails stop during charge, pursuit and gaps between contact windows, and cannot bridge separate attack sessions. Charge strands wrap the right arm. A dive creates terrain fragments at its actual landing surface.

Successful server-confirmed hits create a direction-aligned slice at the victim surface, directional red/dark particles (sparks for iron golems), distinct swing/contact sound layering and a short directional camera impulse. Missed swings produce no body-impact feedback. Existing victim stagger and mass-sensitive knockback remain in control.

The private decoded blade event records establish separate right-hand charge and motion-trail events. They do not establish the original shaders, emitter curves, audio samples or full runtime behavior. These visuals and feedback strengths are authored adaptations; no original effect assets are distributed. This revision has not been visually or gameplay tested, at the user's request.
