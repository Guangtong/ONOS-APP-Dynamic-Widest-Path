Source Code Description

1. Dynamic Widest Path Traffic Engineering

./widest-path-app/pom.xml
	Project dependencies
	Maintained by Guangtong Shen

./widest-path-app/t1_6nodes.py
./widest-path-app/t2_6nodes_easy.py
	Mininet Topology script
	Author: Guangtong Shen

./widest-path-app/t1_6nodes.txt
./widest-path-app/t2_6nodes_easy.txt
	Link Bandwidth File
	Author: Haofan Feng

./widest-path-app/src/main/java/org/ece595/widest/AppComponent.java
	App entrance, importing service, prepare data structures, reading link bandwidth
	Author: Guangtong Shen

./widest-path-app/src/main/java/org/ece595/widest/DynamicWidestRouting.java
	Find the widest path
	Author: Guangtong Shen, Haofan Feng

./widest-path-app/src/main/java/org/ece595/widest/IntentAutoRemoveTask.java
	Remove an idle intent automatically
	Author: Guangtong Shen

./widest-path-app/src/main/java/org/ece595/widest/LinkBandWidthTool.java
	Measure Link bandwidth capapcity and utilization from ONOS API
	Author: Guangtong Shen

./widest-path-app/src/main/java/org/ece595/widest/ReadEdgeParameter.java
	Obtain Link capacity from file
	Author: Haofan Feng

./widest-path-app/src/main/java/org/ece595/widest/WidestPathPacketProcessor.java
	Read packet and install Path intent for it
	Author: Guangtong Shen


2. Multipath Optimal Allocation

./widest-path-app-LP/*
	Same as in the above app, added lp library to pom.xml
	Maintained by Guangtong Shen

./widest-path-app-LP/src/main/java/org/ece595/widest/AppComponent.java
	App entrance, start periodic task of LP
	Author: Guangtong Shen

./widest-path-app-LP/src/main/java/org/ece595/widest/CalculateBandwidthPeriodicTask.java
	Periodicly run LP calculation
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/DemandResult.java
	Data Type to store Demand measured result
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/DynamicWidestRouting.java
	Find the available paths
	Author: Guangtong Shen, Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/InitialDemand.java
	Data Type to store demand information
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/LinkBandWidthTool.java
	Measure Link bandwidth capapcity and utilization from ONOS API
	Author: Guangtong Shen

./widest-path-app-LP/src/main/java/org/ece595/widest/LP.java
	Linear Programming calculation
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/LPPreparation.java
	Prepare data structure used for LP calculation
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/MaximizeThroughput.java
	Helper functions to prepare data structure used for LP calculation
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/MultipathManager.java
	Translate LP result to flow rules
	Author: Guangtong Shen

./widest-path-app-LP/src/main/java/org/ece595/widest/PathResult.java
	Data Type to store path found
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/ReadEdgeParameter.java
	Obtain Link capacity from file
	Author: Haofan Feng

./widest-path-app-LP/src/main/java/org/ece595/widest/WidestpathPakcetProcessor.java
	Read flow demand from each packet
	Author: Haofan Feng





	