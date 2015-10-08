<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />

This file lists the current project committers and assigned maintainers.

To contribute patches to the Trace Compass project, please see the
[contributor guidelines](https://wiki.eclipse.org/Trace_Compass/Contributor_Guidelines).


Current project committers
--------------------------

* Geneviève Bastien <gbastien@versatic.net>
* Bernd Hufmann <bernd.hufmann@ericsson.com>
* Matthew Khouzam <matthew.khouzam@ericsson.com>
* Marc-André Laperle <marc-andre.laperle@ericsson.com>
* Alexandre Montplaisir <alexmonthy@voxpopuli.im>
* Patrick Tassé <patrick.tasse@ericsson.com>


Maintainers
-----------

Every component, plugin or package in Trace Compass can have a
maintainer assigned to them.

Maintainers are expected to review patches posted to Gerrit that
affect the code they are responsible for.

The list below shows the hierarchy of components, plugins and packages,
and the maintainers assigned to each one. A maintainer listed for a
component means they maintain everything under it.

*Consensus* means that no particular maintainer is assigned to this
area of the code by design, and modifications require a consensus
among all committers.

*Open* means that there is nobody specifically maintaining this part
of the code, but the position is available to anyone interested. In the mean
time, the review process for this code is the same as consensus.


### analysis/ ###
    o.e.t.analysis.graph.*         : Geneviève Bastien
    o.e.t.analysis.os.linux.core.* : Alexandre Montplaisir
    o.e.t.analysis.os.linux.ui.*   : Patrick Tassé

### btf/ ###
    Open

### common/ ###
    Consensus

### ctf/ ###
    Matthew Khouzam

### doc/ ###
    Consensus

### gdbtrace/ ###
    Patrick Tassé

### lttng/ ###
    o.e.t.lttng2.control.*           : Bernd Hufmann
    o.e.t.lttng2.{kernel|ust}.core.* : Alexandre Montplaisir
    o.e.t.lttng2.{kernel|ust}.ui.*   : Matthew Khouzam

### pcap/ ###
    Open

### rcp/ ###
    Bernd Hufmann

### releng/ ###
    Consensus

### statesystem/ ###
    o.e.t.segmentstore.* : Matthew Khouzam
    o.e.t.statesystem.*  : Alexandre Montplaisir

### tmf/ ###
    o.e.t.tmf.analysis.xml.* : Geneviève Bastien
    o.e.t.tmf.remote.*       : Bernd Hufmann
    o.e.t.tmf.core.indexer.* : Marc-André Laperle
    o.e.t.tmf.core.* (rest)  : Consensus
    o.e.t.tmf.ui.uml2sd.*    : Bernd Hufmann
    o.e.t.tmf.ui.* (rest)    : Patrick Tassé

