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

package com.analog.lyric.dimple.test.dummySolver;

import com.analog.lyric.dimple.factorfunctions.core.IFactorTable;
import com.analog.lyric.dimple.model.factors.Factor;
import com.analog.lyric.dimple.solvers.core.STableFactorBase;

public class DummyTableFactor extends STableFactorBase
{
	public DummyTableFactor(Factor factor, DummyFactorGraph parent)
	{
		super(factor, parent);
	}

	@Override
	protected boolean createFactorTableOnInit()
	{
		return false;
	}
	
	@Override
	public void doUpdateEdge(int outPortNum)
	{
	}
		
	@Override
	protected void doUpdate()
	{
	}
	@Override
	protected void setTableRepresentation(IFactorTable table)
	{
	}
}
