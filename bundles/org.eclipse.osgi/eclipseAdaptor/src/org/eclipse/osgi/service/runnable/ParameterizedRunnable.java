/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.runnable;

/**
 * Like a java.lang.Runnable, an object which captures a block of code which can 
 * be passed around and executed.  Unlike standard runnables, paramaterized 
 * runnables allow an arbitrary <code>Object</code> to be passed in when the 
 * block is evaluated.
 * 
 * @since 3.0
 */
public interface ParameterizedRunnable {

	/**
	 * Executes the block of code encapsulated by this runnable in the context of
	 * the given object and returns result.  The result may be <code>null</code>.
	 * @param context the context for evaluating the runnable
	 * @return the result of evaluating the runnable in the given context
	 */
	public Object run(Object context);
}
