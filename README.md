# Distributed-LCM
Distributed LCM algorithm that finds closed concepts

# SetUp MPJ Library
1. Download MPJ Express and unpack it. 
2. Set MPJ_HOME and PATH environmental variables:
		export MPJ_HOME=/path/to/mpj/
		export PATH=$MPJ_HOME/bin:$PATH 
		(These above two lines can be added to ~/.bashrc)

# IntelliJ IDEA Run Configuration
Main class: iitmandi.lcm.Main
VM options: -jar ${MPJ_HOME}/lib/starter.jar -np 4
Environment variables: MPJ_HOME=/home/shravan/mpj-v0_44/ (use your MPJ path)