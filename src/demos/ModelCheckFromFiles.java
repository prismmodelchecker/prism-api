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
import java.util.List;

import parser.Values;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLog;
import prism.Result;
import prism.UndefinedConstants;

/**
 * An example class demonstrating how to control PRISM programmatically,
 * through the functions exposed by the class prism.Prism.
 * 
 * This shows how to load a model from a file and model check some properties,
 * either from a file or specified as a string, and possibly involving constants. 
 * 
 * See the README for how to link this to PRISM.
*/
public class ModelCheckFromFiles
{

	public static void main(String[] args)
	{
		new ModelCheckFromFiles().run();
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
			ModulesFile modulesFile = prism.parseModelFile(new File("examples/dice.pm"));
			prism.loadPRISMModel(modulesFile);

			// Parse and load a properties model for the model
			PropertiesFile propertiesFile = prism.parsePropertiesFile(modulesFile, new File("examples/dice.pctl"));

			// Model check the first property from the file
			System.out.println(propertiesFile.getPropertyObject(0));
			Result result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult());

			// Model check the second property from the file
			// (which has an undefined constant, whose value we set to 3)
			List<String> consts = propertiesFile.getUndefinedConstantsUsedInProperty(propertiesFile.getPropertyObject(1));
			String constName = consts.get(0);
			Values vals = new Values();
			vals.addValue(constName, new Integer(3));
			propertiesFile.setUndefinedConstants(vals);
			System.out.println(propertiesFile.getPropertyObject(1) + " for " + vals);
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(1));
			System.out.println(result.getResult());

			// Model check the second property from the file
			// (which has an undefined constant, which we check over a range 0,1,2)
			UndefinedConstants undefConsts = new UndefinedConstants(modulesFile, propertiesFile, propertiesFile.getPropertyObject(1));
			undefConsts.defineUsingConstSwitch(constName + "=0:2");
			int n = undefConsts.getNumPropertyIterations();
			for (int i = 0; i < n; i++) {
				Values valsExpt = undefConsts.getPFConstantValues();
				propertiesFile.setUndefinedConstants(valsExpt);
				System.out.println(propertiesFile.getPropertyObject(1) + " for " + valsExpt);
				result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(1));
				System.out.println(result.getResult());
				undefConsts.iterateProperty();
			}

			// Model check a property specified as a string
			propertiesFile = prism.parsePropertiesString(modulesFile, "P=?[F<=5 s=7]");
			System.out.println(propertiesFile.getPropertyObject(0));
			result = prism.modelCheck(propertiesFile, propertiesFile.getPropertyObject(0));
			System.out.println(result.getResult());

			// Model check an additional property specified as a string
			String prop2 = "R=?[F s=7]";
			System.out.println(prop2);
			result = prism.modelCheck(prop2);
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