//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package demos;

import java.io.File;
import java.io.FileNotFoundException;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ModulesFile;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismPrintStreamLog;
import simulator.SimulatorEngine;

/**
 * An example class demonstrating how to control PRISM programmatically,
 * through the functions exposed by the class prism.Prism.
 * 
 * This shows how to use PRISM's discrete-even simulator to generate some
 * sample paths through a model loaded in from a file.
 * 
 * See the README for how to link this to PRISM.
*/
public class SimulateModel
{

	public static void main(String[] args)
	{
		new SimulateModel().run();
	}

	public void run()
	{
		try {
			// Create a log for PRISM output (hidden or stdout)
			PrismLog mainLog = new PrismDevNullLog();
			//PrismLog mainLog = new PrismFileLog("stdout");

			// Initialise PRISM engine 
			Prism prism = new Prism(mainLog);
			prism.initialise();

			// Parse and load a PRISM model from a file
			ModulesFile modulesFile = prism.parseModelFile(new File("examples/nacl.sm"));
			prism.loadPRISMModel(modulesFile);
			
			// This model has an undefined constants N1 and N2, so we give them values 
			Values vals = new Values();
			vals.addValue("N1", 10);
			vals.addValue("N2", 10);
			prism.setPRISMModelConstants(vals);
			
			// Load the model into the simulator
			prism.loadModelIntoSimulator();
			SimulatorEngine sim = prism.getSimulator();
			
			// Create a new path and take 3 random steps
			// (for debugging purposes, we use sim.createNewPath;
			// for greater efficiency, you could use sim.createNewOnTheFlyPath();
			sim.createNewPath();
			sim.initialisePath(null);
			sim.automaticTransition();
			sim.automaticTransition();
			sim.automaticTransition();
			System.out.println("A random path (3 steps):");
			System.out.println(sim.getPath());
			
			// Create a new path up to 0.01 time units
			// (print a more detailed view this time)
			sim.initialisePath(null);
			sim.automaticTransitions(0.01, false);
			System.out.println("A random path (until 0.01 time units elapse):");
			sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
			
			// Create a new path up until a target expression is satisfied 
			Expression target = prism.parsePropertiesString("na=2").getProperty(0);
			sim.initialisePath(null);
			while (!target.evaluateBoolean(sim.getCurrentState())) {
				sim.automaticTransition();
			}
			System.out.println("\nA random path reaching " + target + ":");
			sim.getPathFull().exportToLog(new PrismPrintStreamLog(System.out), true, ",", null);
			System.out.println("Path time is: " + sim.getPath().getTotalTime());

			// Close down PRISM
			prism.closeDown();

		} catch (FileNotFoundException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}