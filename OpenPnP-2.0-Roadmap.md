# OpenPnP 2.0 Roadmap

* Bug Report: In job proccess if found that one axis lose step and i immediately stop machine(not job). Then i enable machine to perform home, it tried to discard part on nozzle, and hit nozzle
* Bug Report: i do not have autochanger for nozzle and in openpnp v1 show message during job, when i need to manually change nozzle. In openpnp 2, no message, so job not pause and stay work with wrong  real nozzle
* Bug Report: Job.Finished doesn't run when you hit Stop
* Bug Report: also find bug, job placement retries do not reset, if stop job manually
* Remove Apply / Reset, add Undo / Redo
* JobProcessor polish: https://groups.google.com/d/msgid/openpnp/efec8fa2-cef5-4317-85cf-73e841c630b1%40googlegroups.com
* Z Probe improvements: https://github.com/openpnp/openpnp/pull/855#issuecomment-501282321
* Vacuum actuators / pick() and place() refactor: https://github.com/openpnp/openpnp/pull/855#issuecomment-501375164
* Production Tab: https://github.com/openpnp/openpnp/blob/develop/CHANGES.md#2019-05-27
* Upgrade to >= Java 11
* Vector editing + overlays for parts, packages, nozzle tips, feeders.
* Nav View
