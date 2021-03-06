ComputerArchitecture
====================

Washington University in St. Louis, Fall 2012


Our code is executed using the HeuristicsRunner.java file, which is in src/uncas. 

To run the code, cd to the parent directory (group-schedule) and run
./Heuristic_run_SSH

This will run the code with the default parameters.

The HeuristicsRunner.java file runs through all the traces and simulates each experiment, unless set otherwise. It either runs the base experiment, the *_SNR experiment or the *_LAT experiment, by setting heurType to 1, 2 or 3 respectively. You can also increase the multiplier for the latency for the LATENCY_SNR heuristic.

By changing heuristic to any of the valid heuristics in Heuristic.java you can only run specific heuristics. By setting it to heuristic_ALL, it runs through all of them.

You can also set the loop to go through specific trace files, by changing the limits in line 50.

You can set uopLimit to something smaller, but the current setting ensures the program goes through all trace lines for all the traces.

Other parameters from PerfSim can be changed here too, but we generally didn't touch them. 