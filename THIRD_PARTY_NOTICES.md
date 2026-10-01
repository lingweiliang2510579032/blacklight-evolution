# Third-party notices

The Prototype velocity-corrected radial speed effect is inspired by and adapted
from the design of **AnimeSpeedLines**, Copyright (c) 2024 2Retr0, distributed
under the MIT License: https://github.com/2Retr0/AnimeSpeedLines

The implementation in Blacklight Evolution is rewritten for NeoForge 1.21.1's
native `PostChain`, with a distinct shader, radial blur, chromatic separation,
Blacklight tinting, and project-specific activation/synchronization logic.

The complete upstream license below was retrieved from the project's official
GitHub repository on 2026-10-01. Source:
https://github.com/2Retr0/AnimeSpeedLines/blob/89b86ddbe4bc69db8954d39083c9059e9bbbb90c/LICENSE
(license blob SHA-1: `1511dceb22980b24c7b6f2840b7ffc931c0b2bea`).

```text
MIT License

Copyright (c) 2024 2Retr0

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Required runtime dependencies (installed separately)

- GeckoLib 4.9.2 for Minecraft 1.21.1 NeoForge: the installed JAR declares MIT
  and includes `GECKOLIB_LICENSE`.
- Epic Fight 21.17.3.1: the installed JAR declares `Code: GPL-3.0; Assets: All
  Rights Reserved` and includes separate `LICENSE` / `LICENSE-ASSETS` files.

These dependencies are referenced by Gradle and required by the mod metadata;
neither JAR nor its code/assets are shaded into Blacklight's JAR or included in
the Gradle distribution ZIP. Obtain them independently from their official
distribution. Their licenses remain applicable to their own files. This
project's GPL code license and the MIT notice above do not grant redistribution
rights to Epic Fight assets or the private Prototype profiles documented in
`THIRD_PARTY_ASSETS.md`.

The checks above refer to the installed versions named here, not to future
dependency versions. A distributor adding separate dependency JARs to a package
must independently check and retain each dependency's complete license notices.


## Optional Epic ParCool / ParCool integration (R8)

ParCool 3.4.3.3 (LGPL-3.0) is an optional installed mod and compile-only API. Epic ParCool 21.0.0 is independently installed from its official distribution. Blacklight may reference its wall-run animation at runtime, combined with Blacklight's original claw-safe upper body. Neither third-party jar nor any of its animation assets is embedded in Blacklight's distribution or source tree. The optional development runtime jar is placed under ignored `art-work/compat`.

Sources: https://modrinth.com/mod/parcool and https://www.curseforge.com/minecraft/mc-mods/official-epic-fight-x-parcool/files/7490217 . Epic ParCool's distribution metadata says All Rights Reserved even though the project page advertises GPLv3; no permission to redistribute its assets is inferred from the page.
