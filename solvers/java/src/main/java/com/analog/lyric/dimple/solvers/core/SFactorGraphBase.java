/*******************************************************************************
 *   Copyright 2012-2015 Analog Devices, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ********************************************************************************/

package com.analog.lyric.dimple.solvers.core;


import static java.util.Objects.*;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.collect.ExtendedArrayList;
import com.analog.lyric.dimple.environment.DimpleThread;
import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.core.FactorGraphEdgeState;
import com.analog.lyric.dimple.model.core.Node;
import com.analog.lyric.dimple.model.core.NodeId;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.factors.FactorBase;
import com.analog.lyric.dimple.model.factors.FactorList;
import com.analog.lyric.dimple.model.repeated.BlastFromThePastFactor;
import com.analog.lyric.dimple.model.variables.Variable;
import com.analog.lyric.dimple.model.variables.VariableList;
import com.analog.lyric.dimple.options.BPOptions;
import com.analog.lyric.dimple.options.SolverOptions;
import com.analog.lyric.dimple.schedulers.scheduleEntry.IScheduleEntry;
import com.analog.lyric.dimple.solvers.core.multithreading.MultiThreadingManager;
import com.analog.lyric.dimple.solvers.interfaces.IParameterizedSolverFactorGraph;
import com.analog.lyric.dimple.solvers.interfaces.ISolverBlastFromThePastFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverEdge;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;
import com.analog.lyric.dimple.solvers.interfaces.ISolverVariable;
import com.analog.lyric.dimple.solvers.interfaces.SolverNodeMapping;
import com.analog.lyric.util.misc.Internal;
import com.google.common.collect.UnmodifiableIterator;

public abstract class SFactorGraphBase
	<SFactor extends ISolverFactor, SVariable extends ISolverVariable, SEdge extends ISolverEdge>
	extends SNode<FactorGraph>
	implements IParameterizedSolverFactorGraph<SFactor, SVariable, SEdge>
{
	/**
	 * Bits in {@link #_flags} reserved by this class and its superclasses.
	 */
	@SuppressWarnings("hiding")
	protected static final int RESERVED_FLAGS = 0xFFFF0000;
	
	private @Nullable ISolverFactorGraph _parent;
	
	protected int _numIterations = 1;		// Default number of iterations unless otherwise specified
	private @Nullable MultiThreadingManager _multithreader; // = new MultiThreadingManager();
	protected boolean _useMultithreading = false;
	
	/**
	 * Solver factors belonging to {@link this} indexed by {@link Factor}s local index.
	 */
	private final ExtendedArrayList<SFactor> _factors;
	
	/**
	 * Solver variables belonging to {@link this} indexed by {@link Variable}s local index.
	 */
	private final ExtendedArrayList<SVariable> _variables;
	
	/**
	 * Solver subgraphs belonging to {@link this} indexed by each {@link FactorGraph}s local index.
	 */
	private final ExtendedArrayList<ISolverFactorGraph> _subgraphs;
	
	private final @Nullable ExtendedArrayList<SEdge> _edges;
	
	private SolverNodeMapping _solverNodeMapping;
	
	/*--------------
	 * Construction
	 */
	
	protected SFactorGraphBase(FactorGraph graph, @Nullable ISolverFactorGraph parent)
	{
		super(graph);
		_factors = new ExtendedArrayList<>(graph.getFactorCount(0));
		_variables = new ExtendedArrayList<>(graph.getVariableCount(0));
		_subgraphs = new ExtendedArrayList<>(graph.getOwnedGraphs().size());
		_edges = hasEdgeState() ? new ExtendedArrayList<SEdge>(graph.getGraphEdgeCount()) : null;
		_solverNodeMapping = new StandardSolverNodeMapping(this);
		_parent = parent;
	}

	/*----------------------------
	 * ISolverEventSource methods
	 */
	
	@Override
	public SFactorGraphBase<SFactor,SVariable,SEdge> getContainingSolverGraph()
	{
		return this;
	}
	
	/*---------------------
	 * ISolverNode methods
	 */
	
	public FactorGraph getModel()
	{
		return _model;
	}
	
	@Override
	public @Nullable ISolverFactorGraph getParentGraph()
	{
		return _parent;
	}
	
	@Override
	@Internal
	public void setParent(ISolverFactorGraph parent)
	{
		_parent = parent;
		_solverNodeMapping = parent.getSolverMapping();
		_solverNodeMapping.addSolverGraph(this);
	}
	
	@Override
	public ISolverFactorGraph getRootSolverGraph()
	{
		return _solverNodeMapping.getRootSolverGraph();
	}
	
	@Override
	public final SolverNodeMapping getSolverMapping()
	{
		return _solverNodeMapping;
	}
	
	/*----------------------------
	 * ISolverFactorGraph methods
	 */
	
	@SuppressWarnings("null")
	@Override
	public SEdge createEdgeState(FactorGraphEdgeState edge)
	{
		return null;
	}
	
	@Override
	public abstract SFactor createFactor(Factor factor);
	
	@Override
	public abstract SVariable createVariable(Variable variable);
	
	@Override
	public @Nullable SEdge getSolverEdge(FactorGraphEdgeState edge)
	{
		return getSolverEdge(edge, true);
	}
	
	@Override
	public @Nullable SEdge getSolverEdge(int edgeIndex)
	{
		return getSolverEdge(edgeIndex, true);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation simply returns {@link Factor#getSolver()}, which
	 * assumes that the {@code factor}'s model is currently attached to this solver graph.
	 * Subclasses may override this to return a more precise type or to support solvers that
	 * can still be used when they are detached from the model.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ISolverFactor getSolverFactor(Factor factor)
	{
		return _solverNodeMapping.getSolverFactor(factor);
	}

	@Override
	public ISolverFactorGraph getSolverSubgraph(FactorGraph subgraph)
	{
		return _solverNodeMapping.getSolverGraph(subgraph);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The default implementation simply returns {@link Variable#getSolver()}, which
	 * assumes that the {@code variable}'s model is currently attached to this solver graph.
	 * Subclasses may override this to return a more precise type or to support solvers that
	 * can still be used when they are detached from the model.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public ISolverVariable getSolverVariable(Variable variable)
	{
		return _solverNodeMapping.getSolverVariable(variable);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean hasEdgeState()
	{
		return false;
	}

	@Override
	public void moveMessages(ISolverNode other)
	{
		@SuppressWarnings("unchecked")
		SFactorGraphBase<SFactor,SVariable,SEdge> sother = (SFactorGraphBase<SFactor,SVariable,SEdge>)other;
		FactorList otherFactors = sother._model.getFactors();
		FactorList myFactors = _model.getFactors();
		
		final SolverNodeMapping solvers = getSolverMapping();
		
		if (otherFactors.size() != myFactors.size())
			throw new DimpleException("Graphs dont' match");
		
		for (int i = 0; i < myFactors.size(); i++)
		{
			ISolverFactor sf = solvers.getSolverFactor(myFactors.getByIndex(i));
			sf.moveMessages(solvers.getSolverFactor(otherFactors.getByIndex(i)));
		}
		
		VariableList myVars = _model.getVariablesFlat();
		VariableList otherVars = sother._model.getVariablesFlat();
		
		for (int i = 0; i < myVars.size(); i++)
		{
			ISolverVariable sv = solvers.getSolverVariable(myVars.getByIndex(i));
			sv.moveNonEdgeSpecificState(solvers.getSolverVariable((otherVars.getByIndex(i))));
		}
		
	}

	@Override
	public void removeSolverFactor(ISolverFactor sfactor)
	{
		removeSolverNode(sfactor, _factors);
	}
	
	@Override
	public void removeSolverGraph(ISolverFactorGraph subgraph)
	{
		removeSolverNode(subgraph, _subgraphs);
	}
	
	@Override
	public void removeSolverVariable(ISolverVariable svariable)
	{
		// FIXME - what if boundary variable?
		removeSolverNode(svariable, _variables);
	}
	
	private void removeSolverNode(ISolverNode snode, ExtendedArrayList<?> list)
	{
		if (snode.getParentGraph() != this)
		{
			throw new IllegalArgumentException(String.format("'%s' does not belong to '%s'", snode, this));
		}
		
		list.set(NodeId.indexFromLocalId(snode.getModelObject().getLocalId()), null);
	}

	@Override
	public boolean customFactorExists(String funcName)
	{
		return false;
	}


	/**
	 * Sets number of solver iterations.
	 * <p>
	 * Sets {@link #getNumIterations()} and {@link BPOptions#iterations} option
	 * to specified value.
	 */
	@Override
	public void setNumIterations(int numIter)
	{
		setOption(BPOptions.iterations, numIter);
		_numIterations = numIter;
	}
	
	/**
	 * Number of solver iterations
	 * <p>
	 * This is set from {@link BPOptions#iterations} during {@link #initialize}.
	 * <p>
	 * This value is not meaningful to all solvers.
	 */
	@Override
	public int getNumIterations()
	{
		return _numIterations;
	}

	@Override
	public void update()
	{
		for (IScheduleEntry entry : _model.getSchedule())
		{
			entry.update();
		}

	}
	@Override
	public void updateEdge(int outPortNum)
	{
		throw new DimpleException("Not supported");
	}

	@Override
	public void iterate()
	{
		iterate(1);
	}


	@Override
	public void iterate(int numIters)
	{
		final MultiThreadingManager multithreader = _multithreader;
		if (multithreader == null || ! _useMultithreading)
		{
			// *** Single thread
			for (int iterNum = 0; iterNum < numIters; iterNum++)
			{
				update();
				
				// Allow interruption (if the solver is run as a thread); currently interruption is allowed only between iterations, not within a single iteration
				if (Thread.interrupted())
					return;
			}
		}
		else
		{
			// *** Multiple threads
			multithreader.iterate(numIters);
		}
	}
	
	@Override
	public void solveOneStep()
	{
		iterate(_numIterations);
	}
	
	
	@Override
	public void solve()
	{
			
		_model.initialize();
		
		solveOneStep();
		continueSolve();

	}

	@Override
	public void continueSolve()
	{
		
		int i = 0;
		int maxSteps = _model.getNumSteps();
		boolean infinite = _model.getNumStepsInfinite();
		
		while (getModel().hasNext())
		{
			if (!infinite && i >= maxSteps)
				break;
			
			getModel().advance();
			solveOneStep();
			
			i++;
		}
	}

	@Override
	public double getBetheFreeEnergy()
	{
		return getInternalEnergy() - getBetheEntropy();
	}
	
	@Override
	public void estimateParameters(IFactorTable[] tables, int numRestarts,
			int numSteps, double stepScaleFactor) {
		throw new DimpleException("not supported by this solver");
		
	}
	
	@Override
	public void baumWelch(IFactorTable [] tables,int numRestarts,int numSteps)
	{
		throw new DimpleException("not supported by this solver");
	}

	
	@Override
	public double getBetheEntropy()
	{
		double sum = 0;
		
		// Sum up factor entropy
		for (Factor f : _model.getFactors())
			sum += f.getBetheEntropy();
		
		// The following would be unnecessary if we implemented inputs as single node factors
		for (Variable v : _model.getVariablesFlat())
			sum -= v.getBetheEntropy() * (v.getSiblingCount() - 1);
		
		return sum;
	}

	@Override
	public double getScore()
	{
		
		double energy = 0;

		// FIXME: get*Top() methods copy all the objects into a new collection.
		// That should not be necessary.
		
		for (Variable v : getModel().getVariablesTop())
			energy += v.getScore();

		for (FactorBase f : getModel().getFactorsTop())
			energy += f.getScore();

		return energy;
		
	}
	
	@Override
	public double getInternalEnergy()
	{
		double sum = 0;
		
		//Sum up factor internal energy
		for (Factor f : _model.getFactors())
			sum += f.getInternalEnergy();
		
		//The following would be unnecessary if we implemented inputs as single node factors
		for (Variable v : _model.getVariablesFlat())
			sum += v.getInternalEnergy();
		
		return sum;
	}


	@Override
	public ISolverBlastFromThePastFactor createBlastFromThePast(BlastFromThePastFactor f)
	{
		return new SBlastFromThePast(f, this);
	}
	
	@Override
	public void recordDefaultSubgraphSolver(FactorGraph subgraph)
	{
		setSubgraphSolver(subgraph,  subgraph.getSolver());
	}
	
	/***********************************************
	 * 
	 * Threading for Ctrl+C
	 * 
	 ***********************************************/

	// FIXME: this is not really thread safe! There is nothing to prevent you from calling
	// these methods before the previous thread is done.
	
	// For running as a thread, which allows the solver to be interrupted.
	// This is backward compatible with versions of the modeler that call solve() directly.
	private volatile @Nullable Thread _thread;
	private @Nullable Exception _exception = null;	// For throwing exceptions back up to client when solve is running in a thread

	@Override
	public void startContinueSolve()
	{
		final Thread thread = _thread = new DimpleThread(new Runnable()
		{

			@Override
			public void run() {
				try
				{
					continueSolve();
				}
				catch (Exception e)
				{
					_exception = e;					// Pass any exceptions to the main thread so they can be passed to the client
				}
			}
			
		}
		);
		thread.start();
	}

	@Override
	public void startSolveOneStep()
	{
		final Thread thread = _thread = new DimpleThread(new Runnable()
		{

			@Override
			public void run() {
				try
				{
					solveOneStep();
				}
				catch (Exception e)
				{
					_exception = e;					// Pass any exceptions to the main thread so they can be passed to the client
				}
			}
			
		}
		);
		thread.start();
	}
	
	@Override
	public void startSolver()
	{
		final Thread thread = _thread = new DimpleThread(new Runnable()
		{

			@Override
			public void run() {
				try
				{
					solve();
				}
				catch (Exception e)
				{
					_exception = e;					// Pass any exceptions to the main thread so they can be passed to the client
				}
			}
			
		}
		);
		thread.start();
	}
	@Override
	public void interruptSolver()
	{
		final Thread thread = _thread;
		if (thread != null)
		{
			System.out.println(">>> Interrupting solver");
			thread.interrupt();
		}
	}
	@Override
	public boolean isSolverRunning()
	{
		final Exception e = _exception;
		if (e != null)
		{
			_exception = null;				// Clear the exception; the exception should happen only once; no exception if this is called again
			throw new DimpleException(e);						// Pass the exception up to the client
		}
		else
		{
			final Thread thread = _thread;
			if (thread != null)
				return thread.isAlive();
			else
				return false;
		}
	}

	// Allow interruption (if the solver is run as a thread)
	protected void interruptCheck() throws InterruptedException
	{
		try {Thread.sleep(0);}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw e;
		}
	}




	/***********************************************
	 * 
	 * For multi-threaded computation
	 * 
	 ***********************************************/

	@Override
	public void useMultithreading(boolean use)
	{
		if (_multithreader == null)
			throw new DimpleException("Multithreading is not currently supported by this solver.");
		else
			_useMultithreading = use;
		setOption(SolverOptions.enableMultithreading, use);
	}
	
	@Override
	public boolean useMultithreading()
	{
		return _useMultithreading;
	}
	
	
	public MultiThreadingManager getMultithreadingManager()
	{
		final MultiThreadingManager multithreader = _multithreader;
		if (multithreader == null)
			throw new DimpleException("Multithreading is not currently supported by this solver.");
		else
			return multithreader;
	}
	
	protected void setMultithreadingManager(@Nullable MultiThreadingManager manager)
	{
		_multithreader = manager;
	}

	/***********************************************
	 * 
	 * Initialization methods
	 * 
	 ***********************************************/

	/**
	 * Initialize solver graph.
	 * <p>
	 * Default implementation does the following:
	 * <ul>
	 * <li>Initializes {@linkplain #getNumIterations() iterations} and multithreading from options.
	 * <li>Invokes {@linkplain ISolverNode#initialize() initialize} on contents of graph in this order
	 * <ol>
	 * <li>owned solver variables
	 * <li>boundary solver variables (only if this is the root solver graph)
	 * <li>solver factors
	 * <li>solver subgraphs
	 * </ol>
	 * </ul>
	 */
	@Override
	public void initialize()
	{
		_numIterations = getOptionOrDefault(BPOptions.iterations);
		_useMultithreading = getOptionOrDefault(SolverOptions.enableMultithreading);

		initializeSolverEdges();
		
		FactorGraph fg = _model;
		for (Variable variable : fg.getOwnedVariables())
		{
			requireNonNull(getSolverVariable(variable, true)).initialize();
		}
		if (!fg.hasParentGraph())
		{
			for (int i = 0, end = fg.getBoundaryVariableCount(); i <end; ++i)
			{
				getSolverVariable(fg.getBoundaryVariable(i)).initialize();
			}
		}
		for (Factor f : fg.getOwnedFactors())
		{
			requireNonNull(getSolverFactor(f, true)).initialize();
		}
		for (FactorGraph g : fg.getOwnedGraphs())
		{
			requireNonNull(getSolverSubgraph(g, true)).initialize();
		}
	}
	
	/***********************************************
	 * 
	 * Stuff for rolled up graphs
	 * 
	 ***********************************************/

	@Override
	public void moveMessages(ISolverNode other, int portNum, int otherPortNum)
	{
		throw new DimpleException("Not supported");
		
	}
	
	@Override
	public void resetEdgeMessages(int portNum)
	{
		throw new DimpleException("Not supported");
	}

	@Override
	public @Nullable Object getInputMsg(int portIndex)
	{
		return null;
	}

	@Override
	public @Nullable Object getOutputMsg(int portIndex)
	{
		return null;
	}
	
	@Override
	public void setInputMsg(int portIndex, Object obj) {
		throw new DimpleException("Not supported by " + this);
	}
	
	@Override
	public void postAdvance()
	{
		
	}
	@Override
	public void postAddFactor(Factor f)
	{
		
	}
	
	@Override
	public void postSetSolverFactory()
	{
		
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * The default implementation always returns null.
	 */
	@Override
	public @Nullable String getMatlabSolveWrapper()
	{
		return null;
	}


	@Override
	public boolean checkAllEdgesAreIncludedInSchedule()
	{
		return true;	// By default assume all edges must be included unless told otherwise; TODO: should this be the default?
	}
	
	/*--------------------------
	 * Protected helper methods
	 */

	/**
	 * Description name for solver for use in error messages.
	 * @since 0.08
	 */
	abstract protected String getSolverName();
	
	protected DimpleException unsupportedVariableType(Variable var)
	{
		return new DimpleException("'%s' solver does not support %s variables",
			getSolverName(), var.getClass().getSimpleName());
	}
	
	/*---------------
	 * Inner classes
	 * 
	 * These provide iterable views of solver objects in this graph and subgraphs.
	 */
	
	// TODO - make implicit instantiation optional

	private abstract static class SNodeIterator<N extends Node,SN extends ISolverNode>
		extends UnmodifiableIterator<SN>
	{
		private final Iterator<N> _iter;
		
		private SNodeIterator(Collection<N> collection)
		{
			_iter = collection.iterator();
		}
		
		@Override
		public final boolean hasNext()
		{
			return _iter.hasNext();
		}
		
		@Override
		public final SN next()
		{
			return map(_iter.next());
		}
		
		abstract SN map(N node);
	}
	
	private abstract static class SNodes<N extends Node, SN extends ISolverNode>
		extends AbstractCollection<SN>
	{
		final Collection<N> _nodes;
		
		private SNodes(Collection<N> nodes)
		{
			_nodes = nodes;
		}
		
		@Override
		public int size()
		{
			return _nodes.size();
		}
	}
	
	private class OwnedSFactorIterator extends SNodeIterator<Factor,SFactor>
	{
		private OwnedSFactorIterator(Collection<Factor> iterable)
		{
			super(iterable);
			_factors.setSize(iterable.size());
		}
		
		@Override
		public SFactor map(Factor factor)
		{
			return requireNonNull(getSolverFactor(factor, true));
		}
	}
	
	private class OwnedSFactors extends SNodes<Factor, SFactor>
	{
		private OwnedSFactors()
		{
			super(getModelGraph().getOwnedFactors());
		}
		
		@Override
		public Iterator<SFactor> iterator()
		{
			return new OwnedSFactorIterator(_nodes);
		}
	}

	private class OwnedSVarIterator extends SNodeIterator<Variable,SVariable>
	{
		private OwnedSVarIterator(Collection<Variable> iterable)
		{
			super(iterable);
			_variables.setSize(iterable.size());
		}
		
		@Override
		public SVariable map(Variable variable)
		{
			return requireNonNull(getSolverVariable(variable, true));
		}
	}
	
	private class OwnedSVars extends SNodes<Variable, SVariable>
	{
		private OwnedSVars()
		{
			super(getModelGraph().getOwnedVariables());
		}
		
		@Override
		public Iterator<SVariable> iterator()
		{
			return new OwnedSVarIterator(_nodes);
		}
	}

	private class OwnedSubgraphIterator extends SNodeIterator<FactorGraph,ISolverFactorGraph>
	{
		private OwnedSubgraphIterator(Collection<FactorGraph> iterable)
		{
			super(iterable);
			_subgraphs.setSize(iterable.size());
		}
		
		@Override
		public ISolverFactorGraph map(FactorGraph subgraph)
		{
			return instantiateSubgraph(subgraph);
		}
	}
	
	private class OwnedSubgraphs extends SNodes<FactorGraph, ISolverFactorGraph>
	{
		private OwnedSubgraphs()
		{
			super(getModelGraph().getOwnedGraphs());
		}
		
		@Override
		public Iterator<ISolverFactorGraph> iterator()
		{
			return new OwnedSubgraphIterator(_nodes);
		}
	}

	/**
	 * Collection of subgraphs rooted at this in breadth-first order
	 */
	private class RecursiveSubgraphs extends ArrayList<ISolverFactorGraph>
	{
		private static final long serialVersionUID = 1L;

		private RecursiveSubgraphs()
		{
			super();
			
			add(SFactorGraphBase.this);
			
			// Add all subgraphs recursively in bread-first order.
			for (int i = 0; i < size(); ++i)
			{
				ISolverFactorGraph subgraph = get(i);
				addAll(subgraph.getSolverSubgraphs());
			}
		}
	}
	
	private abstract class RecursiveSNodeIterator<SN extends ISolverNode> extends UnmodifiableIterator<SN>
	{
		private final Iterator<ISolverFactorGraph> _sgraphIterator = getSolverSubgraphsRecursive().iterator();
		private Iterator<? extends SN> _snodeIterator = Collections.emptyIterator();
		
		@Override
		public boolean hasNext()
		{
			while (!_snodeIterator.hasNext() && _sgraphIterator.hasNext())
			{
				_snodeIterator = getNodes(_sgraphIterator.next()).iterator();
			}
			
			return _snodeIterator.hasNext();
		}
	
		@Override
		public SN next()
		{
			hasNext();
			
			return _snodeIterator.next();
		}
		
		int count()
		{
			int n = 0;

			while (_sgraphIterator.hasNext())
			{
				n += getNodes(_sgraphIterator.next()).size();
			}
			
			return n;
		}
		
		abstract Collection<? extends SN> getNodes(ISolverFactorGraph graph);
	}
	
	private abstract class RecursiveSNodes<SN extends ISolverNode> extends AbstractCollection<SN>
	{
		@Override
		public abstract RecursiveSNodeIterator<SN> iterator();
		
		@Override
		public int size()
		{
			return iterator().count();
		}
	}

	private class RecursiveSFactorIterator extends RecursiveSNodeIterator<ISolverFactor>
	{
		@Override
		Collection<? extends ISolverFactor> getNodes(ISolverFactorGraph graph)
		{
			return graph.getSolverFactors();
		}
	}
	
	private class RecursiveSFactors extends RecursiveSNodes<ISolverFactor>
	{
		@Override
		public RecursiveSFactorIterator iterator()
		{
			return new RecursiveSFactorIterator();
		}
	}
	
	private class RecursiveSVariableIterator extends RecursiveSNodeIterator<ISolverVariable>
	{
		@Override
		Collection<? extends ISolverVariable> getNodes(ISolverFactorGraph graph)
		{
			return graph.getSolverVariables();
		}
	}
	
	private class RecursiveSVariables extends RecursiveSNodes<ISolverVariable>
	{
		@Override
		public RecursiveSVariableIterator iterator()
		{
			return new RecursiveSVariableIterator();
		}
	}
	
	/*---------
	 * Methods
	 */
	
	public final FactorGraph getModelGraph()
	{
		return this.getModelObject();
	}
	
	public final IParameterizedSolverFactorGraph<SFactor,SVariable,SEdge> getSolverGraph()
	{
		return this;
	}
	
	/**
	 * Unmodifiable collection over owned solver factors, implicitly instantiated if necessary.
	 * @since 0.08
	 */
	@Override
	public Collection<SFactor> getSolverFactors()
	{
		return new OwnedSFactors();
	}
	
	@Override
	public Collection<? extends ISolverFactor> getSolverFactorsRecursive()
	{
		return new RecursiveSFactors();
	}
	
	/**
	 * Unmodifiable collection over owned solver variables, implicitly instantiated if necessary.
	 * @since 0.08
	 */
	@Override
	public Collection<SVariable> getSolverVariables()
	{
		return new OwnedSVars();
	}
	
	@Override
	public Collection<? extends ISolverVariable> getSolverVariablesRecursive()
	{
		return new RecursiveSVariables();
	}
	
	@Override
	public @Nullable ISolverFactorGraph getSolverSubgraph(FactorGraph subgraph, boolean create)
	{
		assertSameGraph(subgraph);
		
		if (create)
		{
			return instantiateSubgraph(subgraph);
		}
		else
		{
			return _subgraphs.getOrNull(NodeId.indexFromLocalId(subgraph.getLocalId()));
		}
	}
	
	@Override
	public Collection<ISolverFactorGraph> getSolverSubgraphs()
	{
		return new OwnedSubgraphs();
	}
	
	@Override
	public Collection<ISolverFactorGraph> getSolverSubgraphsRecursive()
	{
		return new RecursiveSubgraphs();
	}
	
	@SuppressWarnings("unchecked")
	public @Nullable SEdge getSolverEdge(FactorGraphEdgeState edge, boolean create)
	{
		final ExtendedArrayList<SEdge> edges = _edges;
		if (edges == null)
		{
			return null;
		}
		
		final int index = edge.edgeIndexInParent(_model);
		SEdge result = edges.getOrNull(index);
		
		if (result == null)
		{
			if (create)
			{
				FactorGraph factorParent = edge.getFactorParent(_model);
				if (factorParent != _model)
				{
					// If the factor is from a different graph, get the edge from there.
					result = (SEdge) _solverNodeMapping.getSolverGraph(factorParent).getSolverEdge(edge);
				}
				else
				{
					result = this.createEdgeState(edge);
				}
			}
			edges.set(index,  result);
		}
		
		return result;
	}
	
	public @Nullable SEdge getSolverEdge(int edgeIndex, boolean create)
	{
		final ExtendedArrayList<SEdge> edges = _edges;
		if (edges == null)
		{
			return null;
		}
		
		SEdge result = edges.getOrNull(edgeIndex);
		
		if (result == null)
		{
			if (create)
			{
				final FactorGraphEdgeState modelEdge = _model.getGraphEdgeState(edgeIndex);
				if (modelEdge != null)
				{
					return getSolverEdge(modelEdge, true);
				}
			}
		}
		
		return result;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public @Nullable SFactor getSolverFactor(Factor factor, boolean create)
	{
		assertSameGraph(factor);
		
		final int index = NodeId.indexFromLocalId(factor.getLocalId());
		final ExtendedArrayList<SFactor> factors = _factors;
		
		@SuppressWarnings("unchecked")
		SFactor sfactor = factors.getOrNull(index);

		if (sfactor == null || sfactor.getModelObject() != factor)
		{
			if (create)
			{
				if (factor instanceof BlastFromThePastFactor)
				{
					// FIXME - hacky
					sfactor = (SFactor)this.createBlastFromThePast((BlastFromThePastFactor)factor);
				}
				else
				{
					sfactor = this.createFactor(factor);
					sfactor.createMessages();
				}
				if (this == factor.requireParentGraph().getSolver())
				{
					// If parent is default solver for it's graph, make this the default solver for the factor.
					factor.setSolver(sfactor);
				}
			}
			else
			{
				sfactor = null;
			}
			factors.set(index, sfactor);
		}
		
		return sfactor;
	}
	
	public void initializeSolverEdges()
	{
		ExtendedArrayList<SEdge> edges = _edges;
		if (edges != null)
		{
			final int n = getModelGraph().getGraphEdgeCount();
			edges.setSize(n);
			for (int i = 0; i < n; ++i)
			{
				SEdge sedge = getSolverEdge(i, true);
				if (sedge != null)
				{
					sedge.reset();
				}
			}
		}
	}
	
	public ISolverFactorGraph instantiateSubgraph(FactorGraph subgraph)
	{
		assertSameGraph(subgraph);
		
		final int index = NodeId.indexFromLocalId(subgraph.getLocalId());
		final ExtendedArrayList<ISolverFactorGraph> graphs = _subgraphs;
		
		@SuppressWarnings("unchecked")
		ISolverFactorGraph sgraph = graphs.getOrNull(index);
		
		if (sgraph == null || sgraph.getModelObject() != subgraph)
		{
			sgraph = this.createSubgraph(subgraph);
			sgraph.setParent(this);
			graphs.set(index, sgraph);
			if (this == _model.getSolver())
			{
				// If parent is default solver for it's graph, make this the default solver for the subgraph.
				subgraph.setSolver(sgraph);
			}
		}
		
		return sgraph;
	}
	
	@Override
	public @Nullable SVariable getSolverVariable(Variable variable, boolean create)
	{
		assertSameGraph(variable);
		
		final int index = NodeId.indexFromLocalId(variable.getLocalId());
		final ExtendedArrayList<SVariable> variables = _variables;
		
		@SuppressWarnings("unchecked")
		SVariable svar = variables.getOrNull(index);
		
		if (svar == null || svar.getModelObject() != variable)
		{
			if (create)
			{
				svar = this.createVariable(variable);
				if (this == variable.requireParentGraph().getSolver())
				{
					// If parent is default solver for it's graph, make this the default solver for the subgraph.
					variable.setSolver(svar);
				}
				svar.createNonEdgeSpecificState();
				svar.setInputOrFixedValue(variable.getInputObject(), variable.getFixedValueObject());
			}
			else
			{
				svar = null;
			}
			variables.set(index, svar);
		}
		
		return svar;
	}
	
//	public void instantiateAll()
//	{
//		// Instantiate and get solver subgraphs
//		ArrayList<ISolverFactorGraph> sgraphs = new ArrayList<>(getSolverSubgraphsRecursive());
//
//		// Instantiate all solver variables
//		Iterators.size(getSolverVariablesRecursive().iterator());
//
//		// Instantiate solver factors from bottom up (see bug 404)
//		for (int i = sgraphs.size(); --i>=0;)
//		{
//			Iterators.size(sgraphs.get(i).getSolverFactors().iterator());
//		}
//	}
	
	public void setSubgraphSolver(FactorGraph subgraph, @Nullable ISolverFactorGraph sgraph)
	{
		assertSameGraph(subgraph);
		
		final int index = NodeId.indexFromLocalId(subgraph.getLocalId());
		final ExtendedArrayList<ISolverFactorGraph> graphs = _subgraphs;
		if (sgraph != null)
		{
			sgraph.setParent(this);
		}
		graphs.set(index,  sgraph);
	}
	
	/*-----------------
	 * Private methods
	 */
	
	private void assertSameGraph(Node node)
	{
		if (node.getParentGraph() != this.getModelObject())
		{
			throw new IllegalArgumentException(String.format("The %s '%s' does not belong to graph.",
				node.getNodeType().name().toLowerCase(), node));
		}
	}
}
