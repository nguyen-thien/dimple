/*******************************************************************************
*   Copyright 2014 Analog Devices, Inc.
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

package com.analog.lyric.dimple.test.core.parameterizedMessages;

import static com.analog.lyric.util.test.ExceptionTester.*;
import static org.junit.Assert.*;

import org.junit.Test;

import com.analog.lyric.dimple.factorfunctions.MultivariateNormal;
import com.analog.lyric.dimple.model.domains.RealJointDomain;
import com.analog.lyric.dimple.model.values.Value;
import com.analog.lyric.dimple.solvers.core.parameterizedMessages.MultivariateNormalParameters;
import com.analog.lyric.util.test.SerializationTester;

/**
 * 
 * @since 0.06
 * @author Christopher Barber
 */
public class TestMultivariateNormalParameters extends TestParameterizedMessage
{
	@Test
	public void test()
	{
		MultivariateNormalParameters msg = new MultivariateNormalParameters();
		assertTrue(msg.isNull());
		assertFalse(msg.isInInformationForm());
		assertInvariants(msg);
		
		assertEquals(0, msg.getInformationVector().length);
		assertTrue(msg.isInInformationForm());
		assertInvariants(msg);
		
		double[] means = new double[2];
		double[][] covariance = new double[2][2];

		//   mean     covariance
		// | 5.0 |   | 1.0  2.0 |   Not positive definite
		// | 6.0 |   | 2.0  1.0 |
		//
		means[0] = 5.0;
		means[1] = 6.0;
//		covariance[0][0] = 1.0;
//		covariance[1][1] = 1.0;
//		covariance[0][1] = 2.0;
//		covariance[1][0] = 2.0;
//		expectThrow(DimpleException.class, "Matrix is not positive definite", msg, "setMeanAndCovariance",
//			means, covariance);
//		// Message not changed due to exception
//		assertFalse(msg.isInInformationForm());
//		assertTrue(msg.isNull());
//		assertInvariants(msg);
		
		//   mean     covariance                inverse
		// | 5.0 |   | 2.0  1.0 |  det = 3     | 2/3  -1/3 |
		// | 6.0 |   | 1.0  2.0 |              | -1/3  2/3 |
		//
		covariance[0][0] = 2.0;
		covariance[1][1] = 2.0;
		covariance[0][1] = 1.0;
		covariance[1][0] = 1.0;
		msg.setMeanAndCovariance(means, covariance);
		assertFalse(msg.isInInformationForm());
		assertArrayEquals(means, msg.getMean(), 0.0);
		assertArrayEquals(covariance[0], msg.getCovariance()[0], 0.0);
		
		double[][] infoMatrix = msg.getInformationMatrix();
		assertTrue(msg.isInInformationForm());
		assertEquals(2.0/3.0, infoMatrix[0][0], 1e-10);
		assertEquals(2.0/3.0, infoMatrix[1][1], 1e-10);
		assertEquals(-1.0/3.0, infoMatrix[0][1], 1e-10);
		assertEquals(-1.0/3.0, infoMatrix[1][0], 1e-10);
		double[] infoVector = msg.getInformationVector();
		assertArrayEquals(new double[] { 4.0/3.0, 7.0/3.0 }, infoVector, 1e-10);
		assertArrayEquals(means, msg.getMean(), 0.0);
		assertTrue(msg.isInInformationForm());
		assertInvariants(msg);
		
		double[][] covariance2 = msg.getCovariance();
		assertArrayEquals(covariance[0], covariance2[0], 1e-10);
		assertArrayEquals(covariance[1], covariance2[1], 1e-10);
		
		//   mean     covariance                  inverse
		// | 7.0 |   | 4.0  2.0 |  det = 8      |  3/8  -1/4 |
		// | 8.0 |   | 2.0  3.0 |               | -1/3   1/2 |
		//
		means[0] = 7.0;
		means[1] = 8.0;
		covariance[0][0] = 4.0;
		covariance[1][1] = 3.0;
		covariance[0][1] = 2.0;
		covariance[1][0] = 2.0;
		MultivariateNormalParameters msg2 = new MultivariateNormalParameters(means, covariance);
		assertInvariants(msg2);
		
		// Computed these by hand in MATLAB
		assertEquals(.865414626505863, msg.computeKLDivergence(msg2), 1e-10);
		assertEquals(1.50958537349414, msg2.computeKLDivergence(msg), 1e-10);
		
		msg2.setUniform();
		assertFalse(msg2.isNull());
		assertArrayEquals(new double[2], msg2.getMean(), 0.0);
		for (double[] array : msg2.getCovariance())
		{
			assertArrayEquals(new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY }, array, 0.0);
		}
		
		msg2.setNull();
		assertTrue(msg2.isNull());
		assertTrue(msg2.isInInformationForm());
		expectThrow(IllegalArgumentException.class, "Incompatible vector sizes.*", msg, "computeKLDivergence", msg2);
		
		msg2.setInformation(means, covariance);
		assertArrayEquals(means, msg2.getInformationVector(), 1e-14);
		assertTrue(msg2.isInInformationForm());
		assertInvariants(msg2);
		
	}
	
	private void assertInvariants(MultivariateNormalParameters msg)
	{
		assertGenericInvariants(msg);
		
		final int n = msg.getVectorLength();
		assertEquals(n == 0, msg.isNull());
		
		MultivariateNormalParameters msg2 = msg.clone();
		assertEquals(msg.isInInformationForm(), msg2.isInInformationForm());
		assertEquals(n, msg2.getVectorLength());
		
		if (msg.isInInformationForm())
		{
			assertArrayEquals(msg2.getInformationVector(), msg.getInformationVector(), 0.0);
		}
		else
		{
			assertArrayEquals(msg2.getMean(), msg.getMean(), 0.0);
		}

		MultivariateNormalParameters msg3 = SerializationTester.clone(msg);
		assertEquals(msg.isInInformationForm(), msg3.isInInformationForm());
		assertEquals(n, msg3.getVectorLength());
		
		if (msg.isInInformationForm())
		{
			assertArrayEquals(msg.getInformationVector(), msg3.getInformationVector(), 0.0);
		}
		else
		{
			assertArrayEquals(msg.getMean(), msg3.getMean(), 0.0);
		}

		
		double[] means = msg2.getMean();
		assertFalse(msg2.isInInformationForm());
		
		assertArrayEquals(msg2.getMeans(), means, 0.0);
		assertNotSame(means, msg2.getMean());
		assertEquals(n, means.length);
		
		double[] infoVector = msg2.getInformationVector();
		assertEquals(n, infoVector.length);
		
		double[][] covariance = msg2.getCovariance();
		assertFalse(msg2.isInInformationForm());

		assertEquals(n, covariance.length);
		for (int row = 0; row < n; ++row)
		{
			assertEquals(n, covariance[row].length);
			for (int col = 0; col < n; ++col)
			{
				assertEquals(covariance[row][col], covariance[col][row], 1e-14);
			}
		}
		
		double[][] infoMatrix = msg2.getInformationMatrix();
		assertTrue(msg2.isInInformationForm());

		assertEquals(n, infoMatrix.length);
		for (int row = 0; row < n; ++row)
		{
			assertEquals(n, infoMatrix[row].length);
			for (int col = 0; col < n; ++col)
			{
				assertEquals(infoMatrix[row][col], infoMatrix[col][row], 1e-14);
			}
		}
		
		if (n > 0)
		{
			final double normalizer = msg.getNormalizationEnergy();
			MultivariateNormal function = new MultivariateNormal(msg.clone());
			Value value = Value.create(RealJointDomain.create(n));
			final double[] array = value.getDoubleArray();
			for (int i = 0; i < 10; ++i)
			{
				for (int j = 0; j < n; ++j)
				{
					array[j] = testRand.nextDouble();
				}

				final double expectedEnergy = function.evalEnergy(value);
				final double unnormalizedEnergy = msg.evalEnergy(value);
				assertEquals(1.0, expectedEnergy / (unnormalizedEnergy - normalizer), 1e-12);
			}
		}
	}
}
