# Developers Guide

Important information for anyone interested in hacking on or contributing to any of the OpenPnP modules. 

## Licensing and Copyright

OpenPnP uses different licenses for different components. Before submitting changes, please review the license for that module by looking at the LICENSE file in the root directory of the module and decide if it is acceptable to you. Changes under a different license will not be accepted for merge into the repository.

Contributors submitting large pieces of functionality, i.e. whole source files, whole part designs, major rewrites, etc. may assign their own copyright to the submission as long as you are willing to license it under the existing license for that module.

## Committing

Aside from the project maintainers, OpenPnP uses a Fork and Pull style of development. If you would like to submit changes you should fork the project, make your changes and submit a Pull Request. More information about that process is available at https://help.github.com/articles/using-pull-requests.

### Reviews

All requests will be reviewed by the maintainers and will either be merged or comments will be provided as to why not.

### Granularity

Try to commit as little as possible in a given commit. A single feature, a single bug fix, etc. The smaller a commit is the easier it is to integrate into the project and the less likely it is to contain a looked over problem.

## Code Style

Please try to adhere to the existing code style as much as possible. Understand that your code is likely to be reformatted to fit the project standard if it doesn't follow it.

## Module Specific Notes

### GUI

The GUI is written in Java using Swing and WindowBuilder.

The GUI is meant to be generally useful for a variety of PnP machines and users. When making changes, consider how those changes will affect other users and other machines.

Some things to keep in mind:
* Major API changes, especially those in the .spi and .model packages should be discussed with the maintainers before being made. 
* Breaking users' configuration files should never happen. Provide a backwards compatible path.


