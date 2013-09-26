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

package com.analog.lyric.dimple.solvers.gibbs.samplers.conjugate;

import com.analog.lyric.dimple.factorfunctions.NegativeExpGamma;
import com.analog.lyric.dimple.factorfunctions.core.FactorFunction;
import com.analog.lyric.dimple.model.Port;
import com.analog.lyric.dimple.model.RealDomain;
import com.analog.lyric.dimple.solvers.core.SolverRandomGenerator;


public class NegativeExpGammaSampler implements IRealConjugateSampler
{
	@Override
	public double nextSample(Port[] ports, FactorFunction input)
	{
		double alpha = 0;
		double beta = 0;
		
		if (input != null)
		{
			alpha += ((NegativeExpGamma)input).getAlpha();
			beta += ((NegativeExpGamma)input).getBeta();
		}
		
		int numPorts = ports.length;
		for (int port = 0; port < numPorts; port++)
		{
			// The message from each neighboring factor is an array with elements (alpha, beta)
			GammaParameters message = (GammaParameters)(ports[port].getOutputMsg());
			alpha += message.getAlpha();
			beta += message.getBeta();
		}
		
		return nextSample(alpha, beta);
	}

	public double nextSample(double alpha, double beta)
	{
		return -Math.log(SolverRandomGenerator.randGamma.nextDouble(alpha, beta));
	}
	
	// A static factory that creates a sampler of this type
	public static final IRealConjugateSamplerFactory factory = new IRealConjugateSamplerFactory()
	{
		@Override
		public IRealConjugateSampler create() {return new NegativeExpGammaSampler();}
		
		@Override
		public boolean isCompatible(FactorFunction factorFunction)
		{
			if (factorFunction == null)
				return true;
			else if (factorFunction instanceof NegativeExpGamma)
				return true;
			else
				return false;
		}
		
		@Override
		public boolean isCompatible(RealDomain domain)
		{
			return (domain.getLowerBound() == Double.POSITIVE_INFINITY) && (domain.getUpperBound() == Double.POSITIVE_INFINITY);
		}

	};
}
