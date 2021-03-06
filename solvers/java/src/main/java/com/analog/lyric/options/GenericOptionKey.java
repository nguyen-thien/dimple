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

package com.analog.lyric.options;

import java.io.Serializable;

import net.jcip.annotations.Immutable;

/**
 * A generic {@link IOptionKey} implementation.
 */
@Immutable
public class GenericOptionKey<T extends Serializable> extends OptionKey<T>
{
	/*-------
	 * State
	 */
	private static final long serialVersionUID = 1L;
	
	private final Class<T> _type;
	private final T _defaultValue;
	
	/*--------------
	 * Construction
	 */
	
	public GenericOptionKey(Class<?> declaringClass, String name, Class<T> type, T defaultValue)
	{
		super(declaringClass, name);
		_type = type;
		_defaultValue = defaultValue;
	}
	
	/**
	 * Construct new generic option key with specified attributes.
	 * @since 0.08
	 */
	public GenericOptionKey(Class<?> declaringClass, String name, Class<T> type, T defaultValue,
		IOptionKey.Lookup lookupMethod)
	{
		super(declaringClass, name, lookupMethod);
		_type = type;
		_defaultValue = defaultValue;
	}

	/*--------------------
	 * IOptionKey methods
	 */
	
	@Override
	public Class<T> type()
	{
		return _type;
	}

	@Override
	public T defaultValue()
	{
		return _defaultValue;
	}

}
