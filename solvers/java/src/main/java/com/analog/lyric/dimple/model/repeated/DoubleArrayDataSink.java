/*******************************************************************************
*   Copyright 2013 Analog Devices, Inc.
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

package com.analog.lyric.dimple.model.repeated;

import java.util.LinkedList;

import com.analog.lyric.dimple.model.DimpleException;

public class DoubleArrayDataSink implements IDataSink 
{
	private LinkedList<double[]> _data = new LinkedList<double[]>();

	public void push(Object data)
	{
		_data.add((double[])data);
	}
	public double [] getNext()
	{
		if (_data.size() <= 0)
			throw new DimpleException("ACK!");
		
		return _data.pollFirst();
	}

	public boolean hasNext()
	{
		return _data.size() > 0;
	}
	
	public double [][] getArray()
	{
		double [][] retval = new double[_data.size()][];
		int i = 0;
		for (double [] data : _data)
		{
			retval[i] = data;
			i++;
		}
		return retval;
	}
}