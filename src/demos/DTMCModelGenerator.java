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
import parser.ast.DeclarationInt;
import parser.ast.DeclarationType;
import parser.ast.Expression;
import parser.type.Type;
import parser.type.TypeInt;
import prism.ModelGenerator;
import prism.ModelType;
import prism.Prism;
import prism.PrismDevNullLog;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismLog;
import prism.RewardGenerator;

/**
 * An example class demonstrating how to control PRISM programmatically,
 * through the functions exposed by the class prism.Prism.
 * 
 * This shows how to define a model programmatically using the {@link prism.ModelGenerator}
 * interface - in this case a simple Markov chain model of a random walk. 
 * 
 * See the README for how to link this to PRISM.
*/
public class DTMCModelGenerator
{
	public static void main(String[] args)
	{
		new DTMCModelGenerator().run();
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
			RandomWalk modelGen = new RandomWalk(5, 0.6);
			
			// Load the model generator into PRISM,
			// export the model to a dot file (which triggers its construction)
			prism.loadModelGenerator(modelGen);
			prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File("dtmc.dot"));

			// Then do some model checking and print the result
			String[] props = new String[] {
					"P=?[F \"end\"]",
					"P=?[F<=10 \"end\"]",
					"P=?[F \"left\"]",
					"P=?[F \"right\"]",
					"R=?[F \"end\"]"
			};
			for (String prop : props) {
				System.out.println(prop + ":");
				System.out.println(prism.modelCheck(prop).getResult());
			}
			
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
	 * ModelGenerator defining a discrete-time Markov chain (DTMC) model
	 * of a simple random walk whose value varies between -n and n
	 * and the probability of incrementing, rather than decrementing,
	 * the value is p (n and p are both parameters). 
	 */
	class RandomWalk implements ModelGenerator, RewardGenerator
	{
		// Size of walk (state x is in [-n,...,n])
		private int n;
		// Probability of going "right" (incrementing x)
		private double p;
		// Current state being explored
		private State exploreState;
		// Current value of x (state of random walk)
		private int x;

		/**
		 * Construct a new model generator
		 * @param n Size of the (two-sided) random walk
		 * @param p Probability of incrementing
		 */
		public RandomWalk(int n, double p)
		{
			this.n = n;
			this.p = p;
		}

		// Methods for ModelInfo interface

		// The model is a discrete-time Markov chain (DTMC)

		@Override
		public ModelType getModelType()
		{
			return ModelType.DTMC;
		}

		// The model's state comprises one, integer-valued variable x with range -n to +n
		
		@Override
		public List<String> getVarNames()
		{
			return Arrays.asList("x");
		}

		@Override
		public List<Type> getVarTypes()
		{
			return Arrays.asList(TypeInt.getInstance());
		}

		@Override
		public DeclarationType getVarDeclarationType(int i)
		{
			// i will always be 0 since there there is only one variable x
			return new DeclarationInt(Expression.Int(-n), Expression.Int(n));
		}

		// There are three labels: "end", "left" and "right" (x=-n|x=n, x=-n, x=n, respectively)
		
		@Override
		public List<String> getLabelNames()
		{
			return Arrays.asList("end", "left", "right");
		}
		
		// Methods for ModelGenerator interface (rather than superclass ModelInfo)

		@Override
		public State getInitialState() throws PrismException
		{
			// Initially (x) = (0)
			return new State(1).setValue(0, 0);
		}

		@Override
		public void exploreState(State exploreState) throws PrismException
		{
			// Store the state (for reference, and because will clone/copy it later)
			this.exploreState = exploreState;
			// Cache the value of x in this state for convenience
			x = ((Integer) exploreState.varValues[0]).intValue();
		}

		@Override
		public int getNumChoices() throws PrismException
		{
			// This is a DTMC so always exactly one nondeterministic choice (i.e. no nondeterminism)
			return 1;
		}

		@Override
		public int getNumTransitions(int i) throws PrismException
		{
			// End points have a self-loop, all other states have 2 transitions, left and right
			// (Note that i will always be 0 since this is a Markov chain) 
			return (x == -n || x == n) ? 1 : 2;
		}

		@Override
		public Object getTransitionAction(int i, int offset) throws PrismException
		{
			// No action labels in this model
			return null;
		}

		@Override
		public double getTransitionProbability(int i, int offset) throws PrismException
		{
			// End points have a self-loop (with probability 1)
			// All other states go left with probability 1-p and right with probability p
			// We assume that these are transitions 0 and 1, respectively
			// (Note that i will always be 0 since this is a Markov chain) 
			return (x == -n || x == n) ? 1.0 : (offset == 0 ? 1 - p : p);
		}

		@Override
		public State computeTransitionTarget(int i, int offset) throws PrismException
		{
			// End points have a self-loop (with probability 1)
			// All other states go left with probability 1-p and right with probability p
			// We assume that these are transitions 0 and 1, respectively
			// (Note that i will always be 0 since this is a Markov chain) 
			State target = new State(exploreState);
			if (x == -n || x == n) {
				// Self-loop
				return target;
			} else {
				return target.setValue(0, offset == 0 ? x - 1 : x + 1);
			}
		}

		@Override
		public boolean isLabelTrue(int i) throws PrismException
		{
			switch (i) {
			// "end"
			case 0:
				return (x == -n || x == n);
			// "left"
			case 1:
				return (x == -n);
			// "right"
			case 2:
				return (x == n);
			}
			// Should never happen
			return false;
		}

		// Methods for RewardGenerator interface (reward info stored separately from ModelInfo/ModelGenerator)
		
		// There is a single reward structure, r, which just assigns reward 1 to every state.
		// We can use this to reason about the expected number of steps that occur through the random walk. 
		
		@Override
		public List<String> getRewardStructNames()
		{
			return Arrays.asList("r");
		}
		
		@Override
		public double getStateReward(int r, State state) throws PrismException
		{
			// r will only ever be 0 (because there is one reward structure)
			// We assume it assigns 1 to all states.
			return 1.0;
		}
		
		@Override
		public double getStateActionReward(int r, State state, Object action) throws PrismException
		{
			// No action rewards
			return 0.0;
		}
	}
}
