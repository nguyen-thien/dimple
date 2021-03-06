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

package com.analog.lyric.dimple.schedulers.schedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.analog.lyric.dimple.model.core.EdgeState;
import com.analog.lyric.dimple.model.core.FactorGraph;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.model.factors.FactorList;
import com.analog.lyric.dimple.schedulers.IScheduler;
import com.analog.lyric.dimple.schedulers.scheduleEntry.EdgeScheduleEntry;
import com.analog.lyric.dimple.schedulers.scheduleEntry.IScheduleEntry;
import com.analog.lyric.dimple.schedulers.scheduleEntry.NodeScheduleEntry;
import com.analog.lyric.math.DimpleRandomGenerator;

/**
 * @author jeffb
 * 
 *         This is a dynamic schedule, which updates factors in a randomly
 *         chosen sequence with replacement. Prior to each factor update, the
 *         corresponding edges of the connected variables are updated. The
 *         number of factors updated per iteration is equal to the total number
 *         of factors in the graph. However, since the factors are chosen
 *         randomly with replacement, not all factors are necessarily updated in
 *         a single iteration.
 * 
 *         WARNING: This schedule DOES NOT respect any existing sub-graph
 *         scheduler associations. That is, if any sub-graph already has an
 *         associated scheduler, that scheduler is ignored in creating this
 *         schedule.
 */
public class RandomWithReplacementSchedule extends ScheduleBase
{
	private static final long serialVersionUID = 1L;

	/*-------
	 * State
	 */
	
	protected FactorList _factors;
	protected int _numFactors;

	/*--------------
	 * Construction
	 */
	
	public RandomWithReplacementSchedule(FactorGraph factorGraph)
	{
		this(null, factorGraph);
	}

	@SuppressWarnings("null")
	public RandomWithReplacementSchedule(@Nullable IScheduler scheduler, FactorGraph factorGraph)
	{
		super(scheduler, factorGraph);
		initialize();
	}
	
	/*-------------------
	 * ISchedule methods
	 */

	@Override
	public void attach(FactorGraph factorGraph)
	{
		super.attach(factorGraph);
		initialize();
	}

	protected void initialize()
	{
		_factors = getFactorGraph().getNonGraphFactors();
		_numFactors = _factors.size();
		++_version;
	}

	@Override
	public @NonNull FactorGraph getFactorGraph()
	{
		return Objects.requireNonNull(_factorGraph);
	}

	@Override
	public Iterator<IScheduleEntry> iterator()
	{
		ArrayList<IScheduleEntry> updateList = new ArrayList<IScheduleEntry>();

		// One iteration consists of the number of factor updates equaling the total number of factors, even though not all factors will necessarily be updated
		for (int iFactor = 0; iFactor < _numFactors; iFactor++)
		{
			final int factorIndex = DimpleRandomGenerator.rand.nextInt(_numFactors);
			final Factor f = ((ArrayList<Factor>)_factors.values()).get(factorIndex);
			final FactorGraph fg = f.requireParentGraph();
			for (EdgeState edge : f.getSiblingEdgeState())
			{
				updateList.add(new EdgeScheduleEntry(edge.getVariable(fg), edge.getVariableToFactorEdgeNumber()));
			}
			updateList.add(new NodeScheduleEntry(f));
		}

		return updateList.iterator();
	}

}
