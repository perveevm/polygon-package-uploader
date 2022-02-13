# Polygon package uploader

Quite often it happens that you have a problem archive with tests, checker, solutions, problem statements, etc.
from some past programming contest held by someoue else, and you want to upload it into Polygon platform for future use.

This utility can help you to reduce routine work of uploading all this stuff into Polygon and do everything for you!

## Build

Use `install.sh` to install the utility. It will save path to jar directory, build `.jar` file and save the newest version of the utility.
All config files fill be located at `~/polygon-package-uploader`. It also will create a symlink in `/usr/local/bin` to `upload.sh` script.

## Usage

Use `polygon-uploader` (the name of created symlink) to run the script. There are two modes for now: `polygon-uploader init` and `polygon-uploader upload`.
Call them to see an informative help message.

## Currently supported problem archive types

- Polygon archive
- PCMS2 archive in semi-automatic mode (without interactive problems for now)
- To be continued...

## Authors

- [Mike Perveev](https://github.com/perveevm), ITMO University, Lipetsk Strategy Educational Center
- [Konstantin Frolov](https://github.com/Nybik), HSE University, Lipetsk Strategy Educational Center
