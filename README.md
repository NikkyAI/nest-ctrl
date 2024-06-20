## Nestdrop Visuals Controlpanel

# setup and building

needed: JDK? probably java 19+

# required software

## Nestdrop PRO V2
https://www.nestimmersion.ca/nestdrop.php
tested with `V2.x.0.12`

## Carabiner
https://github.com/Deep-Symmetry/carabiner/releases
tested with `v1.1.6`

launch `Carabiner_Win_x64.exe` in a terminal before running the controlpanel

# Configuring Nestdrop location

create a `.emv` folder in the working directory (repo root when running via gradle)

`~/` is supported as a shorthand for user home path
```.env
NESTDROP=~/VJ/NestDropProV2
```

# running from source / IDE

```bash
./gradlew desktop
```
