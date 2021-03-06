/*******************************************************************************
*   Copyright 2012 Analog Devices, Inc.
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

import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.events.SolverEvent;
import com.analog.lyric.dimple.exceptions.DimpleException;
import com.analog.lyric.dimple.model.domains.Domain;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.variables.Variable;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.IParameterizedMessage;
import com.analog.lyric.dimple.solvers.interfaces.ISolverEdgeState;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactorGraph;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;
import com.analog.lyric.dimple.solvers.interfaces.ISolverVariable;
import com.analog.lyric.dimple.solvers.interfaces.SolverNodeMapping;

public abstract class SVariableBase<MVariable extends Variable> extends SNode<MVariable> implements ISolverVariable
{
	/*-----------
	 * Constants
	 */
	
	/**
	 * Bits in {@link #_flags} reserved by this class and its superclasses.
	 */
	@SuppressWarnings("hiding")
	protected static final int RESERVED_FLAGS = 0xFFF00000;
	
	protected final ISolverFactorGraph _parent;
	
	/*--------------
	 * Construction
	 */
	
	public SVariableBase(MVariable var, ISolverFactorGraph parent)
	{
		super(var);
		_parent = parent;
	}
	
	/*---------------------
	 * ISolverNode methods
	 */

	@Override
	public ISolverFactorGraph getContainingSolverGraph()
	{
		return _parent;
	}
	
	@Override
	public ISolverFactorGraph getParentGraph()
	{
		return _parent;
	}
	
	@Override
	public ISolverFactor getSibling(int siblingNumber)
	{
		final Factor sibling = _model.getSibling(siblingNumber);
		return getSolverMapping().getSolverFactor(sibling);
	}
	
	@Override
	public @Nullable ISolverEdgeState getSiblingEdgeState(int siblingNumber)
	{
		return _parent.getSolverEdge(_model.getSiblingEdgeIndex(siblingNumber));
	}
	
	/**
	 * Subclasses can use this to implement {@link #getSiblingEdgeState(int)}.
	 * <p>
	 * Subclasses that cast their return types should call this instead of calling super
	 * to avoid the chain of super calls.
	 * @since 0.08
	 */
	@SuppressWarnings("null")
	protected final ISolverEdgeState getSiblingEdgeState_(int siblingNumber)
	{
		return _parent.getSolverEdge(_model.getSiblingEdgeIndex(siblingNumber));
	}
	
	@Override
	public SolverNodeMapping getSolverMapping()
	{
		return _parent.getSolverMapping();
	}
	
	@Override
	public Object getValue()
	{
		throw new DimpleException("not supported");
	}

	@Override
	public void setGuess(@Nullable Object guess)
	{
		if (guess != null)
		{
			throw new DimpleException("not supported");
		}
	}

	@Override
	public Object getGuess()
	{
		throw new DimpleException("not supported");
	}

	@Override
	public double getScore()
	{
		throw new DimpleException("not supported");
	}

	@Override
	public double getInternalEnergy()
	{
		throw new DimpleException("not supported");
	}

	@Override
	public double getBetheEntropy()
	{
		throw new DimpleException("not supported");
	}
	
	@Override
    public void moveNonEdgeSpecificState(ISolverNode other)
    {
    	
    }
	
	@Override
	public void createNonEdgeSpecificState()
	{
		
	}
	
	/*-------------------------
	 * ISolverVariable methods
	 */
	
	@Override
	public Domain getDomain()
	{
		return _model.getDomain();
	}
	
	/*---------------
	 * SNode methods
	 */
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The {@link SVariableBase} implementation returns a {@link VariableToFactorMessageEvent}.
	 */
	@Override
	protected @Nullable SolverEvent createMessageEvent(
		int edge,
		@Nullable IParameterizedMessage oldMessage,
		IParameterizedMessage newMessage)
	{
		return new VariableToFactorMessageEvent(this, edge, oldMessage, newMessage);
	}


	@Override
	protected Class<? extends SolverEvent> messageEventType()
	{
		return VariableToFactorMessageEvent.class;
	}
	
	/*--------------------
	 * Deprecated methods
	 */

	@Deprecated
	@Override
	public @Nullable Object getInputMsg(int portIndex)
	{
		ISolverEdgeState sedge = getSiblingEdgeState(portIndex);
		return sedge != null ? sedge.getFactorToVarMsg() : null;
	}

	@Deprecated
	@Override
	public @Nullable Object getOutputMsg(int portIndex)
	{
		ISolverEdgeState sedge = getSiblingEdgeState(portIndex);
		return sedge != null ? sedge.getVarToFactorMsg() : null;
	}
}
