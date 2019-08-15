# prism-api

This repo contains example code for connecting to PRISM programmatically.
You will need to separately download a copy of PRISM to connect to.
Instructions are given below for building/running the code.
Browse the example classes in the ``src`` directory to see the example code itself.

## Basic instructions

Download a copy of PRISM and build it

* ``git clone https://github.com/prismmodelchecker/prism prism``
* ``cd prism/prism``
* ``make``

Download the ``prism-api`` repo and build the examples

* ``cd ../..``
* ``git clone https://github.com/prismmodelchecker/prism-api``
* ``cd prism-api``
* ``make``
* ``make test``

## Further instructions

The second part of the above assumes that PRISM is in a directory called ``prism`` one level up.
If you want to use a PRISM distribution located elsewhere, build like this:

* ``make PRISM_DIR=/some/copy/of/prism``

Running ``make test`` is equivalent to calling ``bin/run``.
You can also call this directly, changing the location of PRISM if required:

* ``PRISM_DIR=/some/copy/of/prism bin/run``

By default, the ``run`` script runs code in class ``demos.ModelCheckFromFiles``.
You can change this as follows:

* ``PRISM_MAINCLASS=demos.AnotherTest bin/run``

You can create your own code in the ``src`` directory and then compile/run it as above.
Or, have a look at the ``run`` script to see what is needed to connect to PRISM
from your own separate code/software. Essentially you need to:

* set up the Java classpath to include PRISM's classes
  (from the ``classes`` directory and/or jars in the ``lib directory`` of a distribution)
* set ``LD_LIBRARY_PATH`` (on Linux, or ``DYLD_LIBRARY_PATH`` on Macs)
  to read PRISM's native libraries from the ``lib`` directory at runtime)
  
The script also takes care of an issue with recent Macs where ``DYLD_LIBRARY_PATH``
is not read when the ``java`` binary is a symlink.


## Versioning

The `prism-api` repo is generally kept in synch with the master branch of
the main `prism` repo in terms of any changes made to the API.
If you want to roll back to an old version of PRISM, you can, for example, do:

* ``git checkout v4.5``

after each of the `git clone` instructions above. This switches to the `v4.5` release of PRISM
and to a matching branch on `prism-api`, which should be the latest version known to work
with that release.

