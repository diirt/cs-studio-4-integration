/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pvtable.model;

import org.csstudio.vtype.pv.PV;
import org.epics.vtype.VDouble;
import org.epics.vtype.VEnum;
import org.epics.vtype.VFloat;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;

/** Saved value of a scalar table item
 *  @author Kay Kasemir
 */
public class SavedScalarValue extends SavedValue
{
    final private String saved_value;
    
    /** Initialize from text
     *  @param value_text
     */
    public SavedScalarValue(final String value_text)
    {
        saved_value = value_text;
    }
    
    /** {@inheritDoc} */
    public boolean isEqualTo(final VType current_value, final double tolerance) throws Exception
    {
        if (current_value == null)
            return true;
        if (current_value instanceof VNumber)
        {
            final double v1 = ((VNumber)current_value).getValue().doubleValue();
            final double v2 = Double.parseDouble(saved_value);
            return Math.abs(v2 - v1) <= tolerance;
        }
        if (current_value instanceof VString)
        {
            final String v1 = ((VString)current_value).getValue();
            return v1.equals(saved_value);
        }
        if (current_value instanceof VEnum)
        {
            final int v1 = ((VEnum)current_value).getIndex();
            final int v2 = Integer.parseInt(saved_value);
            return Math.abs(v2 - v1) <= tolerance;
        }
        throw new Exception("Cannot compare against unhandled type " + current_value.getClass().getName());
    }
    
    /** {@inheritDoc} */
    public void restore(final PV pv) throws Exception
    {
        // Determine what type to write based on current value of the PV
        final VType pv_type = pv.read();
        if ((pv_type instanceof VDouble) || (pv_type instanceof VFloat))
            pv.write(Double.parseDouble(saved_value));
        else if (pv_type instanceof VNumber)
            // Parse as double to allow "1e10" or "100.0"
            // If it's "3.14", only "3" will of course be restored.
            pv.write((long)Double.parseDouble(saved_value));
        else // Write as text
            pv.write(saved_value);
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return saved_value;
    }
}
