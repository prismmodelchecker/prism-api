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
import java.util.Arrays;
import java.util.List;

import parser.State;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeInt;
import prism.DefaultModelGenerator;
import prism.ModelType;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;

/**
 * An example class demonstrating how to control PRISM programmatically,
 * through the functions exposed by the class prism.Prism.
 * 
 * This shows how to define a model programmatically using the {@link prism.ModelGenerator}
 * interface - in this case a simple Markov decision process model of movement around a grid.
 * 
 * See the README for how to link this to PRISM.
*/
public class MDPModelGenerator
{
	public static void main(String[] args)
	{
		new MDPModelGenerator().run();
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

			// Create a model generator to specify the model that PRISM should build
			// (in this case a simple random walk)
			GridModel modelGen = new GridModel(4);
			
			// Load the model generator into PRISM,
			// export the model to a dot file (which triggers its construction)
			prism.loadModelGenerator(modelGen);
			prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File("mdp.dot"));

			// Then do some model checking and print the result
			String[] props = new String[] {
					"Pmax=?[F \"target\"]",
					"Rmin=?[F (\"target\"|failed=true)]"
			};
			for (String prop : props) {
				System.out.println(prop + ":");
				System.out.println(prism.modelCheck(prop).getResult());
			}
			
			// Now check the first property again,
			// but this time export the optimal strategy ("adversary") too
			prism.setExportAdv(Prism.EXPORT_ADV_MDP);
			prism.setExportAdvFilename("adv.tra");
			System.out.println(prism.modelCheck("Pmax=?[F \"target\"]").getResult());
			// Also export the MDP states and labels
			prism.exportStatesToFile(Prism.EXPORT_PLAIN, new File("adv.sta"));
			prism.exportLabelsToFile(null, Prism.EXPORT_PLAIN, new File("adv.lab"));
			// Then load it back in, re-verify the property (as a sanity check)
			// and export the induced model as a dot file
			// (really, the induced model is a DTMC, but we keep it as an MDP
			// so that we can see the action labels taken in the optimal strategy)
			prism.setEngine(Prism.HYBRID);
			prism.loadModelFromExplicitFiles(new File("adv.sta"), new File("adv.tra"), new File("adv.lab"), null, ModelType.MDP);
			System.out.println(prism.modelCheck("Pmax=?[F \"target\"]").getResult());
			prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File("adv.dot"));

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
	
	/**
	 * ModelGenerator defining a Markov decision process (MDP) model
	 * of movement around an n x n grid.
	 */
	class GridModel extends DefaultModelGenerator
	{
		// Size of grid (n x n)
		private int n;
		// Current state being explored
		private State exploreState;
		// Current value of x (x coord: 1<=x<=n)
		private int x;
		// Current value of y (y coord: 1<=y<=n)
		private int y;
		// Current value of failed
		private boolean failed;
		// Labels for 4 actions
		private String actions[] = { "north", "east", "south", "west" };

		/**
		 * Construct a new model generator
		 * @param n Size of grid (n x n)
		 */
		public GridModel(int n)
		{
			this.n = n;
		}

		// Methods for ModelInfo interface

		// The model is a Markov decision process (MDP)

		@Override
		public ModelType getModelType()
		{
			return ModelType.MDP;
		}

		// The model's state comprises two integer-valued variables, x and y,
		// indicating current grid coordinates, and a boolean, failed, indicating a failure state. 
		
		@Override
		public List<String> getVarNames()
		{
			return Arrays.asList("x", "y", "failed");
		}

		@Override
		public List<Type> getVarTypes()
		{
			return Arrays.asList(TypeInt.getInstance(), TypeInt.getInstance(), TypeBool.getInstance());
		}

		@Override
		public DeclarationType getVarDeclarationType(int i) throws PrismException
		{
			switch (i) {
			// x or y have finite range, so give a declaration
			case 0:
			case 1:
				return new DeclarationInt(Expression.Int(1), Expression.Int(n));
			// for anything else (actually, just "failed"), use a default
			default:
				return super.getVarDeclarationType(i);
			}
		}
		
		// There is just one label: "goal" (top-right corner)
		
		@Override
		public List<String> getLabelNames()
		{
			return Arrays.asList("target");
		}
		
		// Methods for ModelGenerator interface (rather than superclass ModelInfo)

		@Override
		public State getInitialState() throws PrismException
		{
			// Initially (x,y,failed) = (1,1,false), i.e., bottom-left corner, no failure 
			return new State(3).setValue(0, 1).setValue(1, 1).setValue(2, false);
		}

		@Override
		public void exploreState(State exploreState) throws PrismException
		{
			// Store the state (for reference, and because will clone/copy it later)
			this.exploreState = exploreState;
			// Cache the value of x,y,failed in this state for convenience
			x = ((Integer) exploreState.varValues[0]).intValue();
			y = ((Integer) exploreState.varValues[1]).intValue();
			failed = ((Boolean) exploreState.varValues[2]).booleanValue();
		}

		@Override
		public int getNumChoices() throws PrismException
		{
			// All 4 actions ("north", "east", "south", "west") are always available
			return 4;
		}

		@Override
		public int getNumTransitions(int i) throws PrismException
		{
			// Usually 2 transitions (non-failure, failure)
			// unless we have already failed, in which case there is just 1 
			return failed ? 1 : 2;
		}

		@Override
		public Object getTransitionAction(int i, int offset) throws PrismException
		{
			// Action labels are the same in every state
			return actions[i];
		}

		@Override
		public double getTransitionProbability(int i, int offset) throws PrismException
		{
			// If we have already failed, there is just one transition
			// (a self-loop) and this occurs with probability 1
			if (failed) {
				return 1.0;
			}
			else {
				// We assume that failure is more likely towards the left part of the grid
				double probFail = ((double) (n - x)) / n;
				// Regardless of the action (i.e., for any i),
				// transitions 0 and 1 correspond to non-failure and failure, respectively.
				return offset == 0 ? 1 - probFail : probFail;
			}
		}

		@Override
		public State computeTransitionTarget(int i, int offset) throws PrismException
		{
			State target = new State(exploreState);
			// If we have already failed, there is just one transition (a self-loop)
			if (failed) {
				return target;
			}
			else {
				// Transitions 0 and 1 correspond to non-failure and failure, respectively.
				// Non-failure
				if (offset == 0) {
					switch (i) {
					case 0:
						// North
						target.setValue(1, y < n ? y + 1 : y);
						return target;
					case 1:
						// East
						target.setValue(0, x < n ? x + 1 : x);
						return target;
					case 2:
						// South
						target.setValue(1, y > 1 ? y - 1 : y);
						return target;
					case 3:
						// West
						target.setValue(0, x > 1 ? x - 1 : x);
						return target;
					}
				}
				// Failure
				else {
					// Same outcome, regardless of chosen action: failed = true
					target.setValue(2, true);
					return target;
				}
			}
			// Never happens
			return target;
		}

		@Override
		public boolean isLabelTrue(int i) throws PrismException
		{
			switch (i) {
			case 0:
				// "target" (top-right corner)
				return x == n && y == n;
			default:
				throw new PrismException("Label number \"" + i + "\" not defined");
			}
		}

		// Methods for RewardGenerator interface (reward info stored separately from ModelInfo/ModelGenerator)
		
		// There is a single reward structure, r, which just assigns reward 1 to every transition.
		// We can use this to reason about the expected number of steps taken through the grid. 
		
		@Override
		public List<String> getRewardStructNames()
		{
			return Arrays.asList("r");
		}
		
		
		@Override
		public double getStateReward(int r, State state) throws PrismException
		{
			// No state rewards 
			return 0.0;
		}
		
		@Override
		public double getStateActionReward(int r, State state, Object action) throws PrismException
		{
			// r will only ever be 0 (because there is one reward structure)
			// We assume it assigns 1 to all transitions.
			return 1.0;
		}
	}
}
