/*******************************************************************************
*   Copyright 2015 Analog Devices, Inc.
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

package math.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.analog.lyric.math.DimpleRandom;

/**
 * 
 * @since 0.08
 * @author Christopher Barber
 */
public class TestDimpleRandom
{
	@Test
	public void testSeed()
	{
		DimpleRandom r1 = new DimpleRandom();
		DimpleRandom r2 = new DimpleRandom(r1.getSeed());

		// Trivial test to make sure that setting seed replicates results.
		assertEquals(r1.getSeed(), r2.getSeed());
		assertEquals(r1.nextDouble(), r2.nextDouble(), 0.0);
	}
}
