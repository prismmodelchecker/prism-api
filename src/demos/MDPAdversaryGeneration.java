//==============================================================================
//	
//	Copyright (c) 2017-
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

import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.ModelType;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismSettings;
import prism.Result;

/**
 * An example class demonstrating how to control PRISM programmatically,
 * through the functions exposed by the class prism.Prism.
 * 
 * This shows how to load an MDP model from a file, model check a property,
 * and export a corresponding optimal adversary to a file.
 * As a sanity check, this is then read back in (as a DTMC) and model checked again.
 * 
 * See the README for how to link this to PRISM.
*/
public class MDPAdversaryGeneration
{

	public static void main(String[] args)
	{
		new MDPAdversaryGeneration().run();
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

			// Parse and load a PRISM model (an MDP) from a file
			ModulesFile modulesFile = prism.parseModelFile(new File("examples/robot.prism"));
			prism.loadPRISMModel(modulesFile);

			// Export the states of the model to a file
			prism.exportStatesToFile(Prism.EXPORT_PLAIN, new File("adv.sta"));
			prism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, new File("adv.lab"));
			
			// Parse and load a properties model for the model
			PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File("examples/robot.props"));

			// Configure PRISM to export an optimal adversary to a file when model checking an MDP 
			prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV, "DTMC");
			prism.getSettings().set(PrismSettings.PRISM_EXPORT_ADV_FILENAME, "adv.tra");
			
			// Model check the first property from the file (Pmax=? [ F "goal1" ])
			System.out.println(propertiesFile.getPropertyObject(0));
			Result result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult());

			// As a sanity check, load the adversary (a DTMC) back into PRISM
			// and model check the second property from the file (P=? [ F "goal1" ])
			System.out.println(propertiesFile.getPropertyObject(1));
			prism.loadModelFromExplicitFiles(new File("adv.sta"), new File("adv.tra"), new File("adv.lab"), null, ModelType.DTMC);
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(1));
			System.out.println(result.getResult());
					
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