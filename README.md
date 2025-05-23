# NEST CTRL

> Nestdrop Control Panel

# Download

get the latest build from the [nightly](https://github.com/NikkyAI/nest-ctrl/releases/tag/nightly) tag

# setup and building

needed: JDK/JRE, java 21+

with https://scoop.sh/
```bash
scoop bucket add java
scoop install openjdk
````

# required software

## Nestdrop PRO V2

https://www.nestimmersion.ca/nestdrop.php

tested with `V2.2.1.3`

# Nestdrop setup

## sprites

Nestdrop tends to do random stuff if any image is laying in the sprites root folder `NestDropProV2\Plugins\Milkdrop2\Sprites`
it is recommended to move the files there to a subfolder

(it makes the IDs assigned to sprites unpredictable and wil lead to the wrong sprites being loaded)

## OSC

OSC input and output needs to be enabled  
input port: `8000`  
output ip: `127.0.0.1`   
output port: `8001`  

## Configuring Nestdrop location

edit `~/.nestctrl/.env` (automatically created when running NESTCTRL the first time)

`~/` is supported as a shorthand for user home path
adjust the `NESTDROP_PATH` as required, for more info see [dotenv keys](#dotenv-keys)
```.env
NESTDROP=~/VJ/NestDropProV2
NESTDROP_PROFILE=DefaultUserProfile.xml
```

## IMG sprites

image sprites are getting loaded by parsing `Plugins\Milkdrop2\Sprites` folder

## SPOUT sprites

spout sprites are loaded from the first queue with `spout` in its name for each deck

... at least until UI gets added to pick them

# dotenv keys

the dotenv file is usually placed

| key                      | default                  | notes                         |
|--------------------------|--------------------------|-------------------------------|
| `NESTDROP_PATH`          | `~/VJ/NestDropProV2`     |                               |
| `NESTDROP_PROFILE`       | `DefaultUserProfile.xml` |                               |
| `DEBUG`                  | `false`                  | dev-only, reduces performance |
| `NESTCTRL_CONFIG_FOLDER` | `~/.nesctrl`             | dev-only, may break things    |

# running from source / IDE

 a .env file in the working directory (the repository root by default) 
 can be used to test with a separate nestdrop installation by setting `NESTCTRL_CONFIG_FOLDER`

```bash
./gradlew desktop
```
