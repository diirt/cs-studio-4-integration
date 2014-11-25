/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser2.model;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.Optional;

import org.csstudio.apputil.xml.DOMHelper;
import org.csstudio.apputil.xml.XMLWriter;
import org.csstudio.swt.rtplot.TraceType;
import org.csstudio.swt.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser2.persistence.XMLPersistence;
import org.csstudio.trends.databrowser2.preferences.Preferences;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Element;

/** Base of {@link PVItem} and {@link FormulaItem},
 *  i.e. the items held by the {@link Model}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ModelItem
{
    /** Name by which the item is identified: PV name, formula */
    private volatile String name;

    /** Model that contains this item. Empty while not assigned to a model
     */
    protected Optional<Model> model = Optional.empty();

    /** Preferred display name, used in plot legend */
    private volatile String display_name;

    /** Show item's samples? */
    private volatile boolean visible = true;

    /** RGB for item's color
     *  <p>
     *  Technically, swt.graphics.RGB adds a UI dependency to the Model.
     *  As long as the Model can still run without a Display
     *  or Shell, this might be OK.
     */
    private volatile RGB rgb = null;

    /** Line width [pixel] */
    private volatile int line_width = Preferences.getLineWidths();

    /** How to display the trace */
    private volatile TraceType trace_type = Preferences.getTraceType();

    /** Y-Axis */
    private volatile AxisConfig axis = null;

    /** Sample that is currently selected, for example via cursor */
    private volatile Optional<PlotDataItem<Instant>> selected_sample = Optional.empty();

    /** Initialize
     *  @param name Name of the PV or the formula
     */
    public ModelItem(final String name)
    {
        this.name = name;
        this.display_name = name;
    }

    /** @return Model that contains this item */
    public Optional<Model> getModel()
    {
        return model;
    }

    /** Called by Model to add item to Model or remove it from model.
     *  Should not be called by other code!
     *  @param model Model to which item was added or <code>null</code> when removed
     */
    void setModel(final Model model)
    {
        if (this.model.equals(model))
            throw new RuntimeException("Item re-assigned to same model: " + name);
        this.model = Optional.ofNullable(model);
    }

    /** @return Name of this item (PV, Formula, ...), may contain macros */
    public String getName()
    {
        return name;
    }

    /** @return Name of this item (PV, Formula, ...) with all macros resolved */
    public String getResolvedName()
    {
    	if (model.isPresent())
    	    return model.get().resolveMacros(name);
    	else
    		return name;
    }

    /** @param new_name New item name
     *  @see #getName()
     *  @return <code>true</code> if name was actually changed
     *  @throws Exception on error (cannot create PV for new name, ...)
     */
    public boolean setName(String new_name) throws Exception
    {
        new_name = new_name.trim();
        if (new_name.equals(name))
            return false;
        name = new_name;
        fireItemLookChanged();
        return true;
    }

    /** @return Preferred display name, used in plot legend.
     *  May contain macros.
     */
    public String getDisplayName()
    {
        return display_name;
    }

    /** @return Preferred display name, used in plot legend,
     *          with macros resolved.
     */
    public String getResolvedDisplayName()
    {
    	if (model.isPresent())
    	    return model.get().resolveMacros(display_name);
    	else
    		return display_name;
    }

    /** @param new_display_name New display name
     *  @see #getDisplayName()
     */
    public void setDisplayName(String new_display_name)
    {
        new_display_name = new_display_name.trim();
        if (new_display_name.equals(display_name))
            return;
        display_name = new_display_name;
        fireItemLookChanged();
    }

    /** @return <code>true</code> if item should be displayed */
    public boolean isVisible()
    {
        return visible;
    }

    /** @param visible Should item be displayed? */
    public void setVisible(final boolean visible)
    {
        if (this.visible == visible)
            return;
        this.visible = visible;
        if (model.isPresent())
            model.get().fireItemVisibilityChanged(this);
    }

    /** If (!) assigned to a model, inform it about a configuration change */
    protected void fireItemLookChanged()
    {
        if (model.isPresent())
            model.get().fireItemLookChanged(this);
    }

    /** Get item's color.
     *  For new items, the color is <code>null</code> until it's
     *  either set via setColor() or by adding it to a {@link Model}.
     *  @return Item's color
     *  @see #setColor(RGB)
     */
    public RGB getColor()
    {
        return rgb;
    }

    /** @param new_rgb New color for this item */
    public void setColor(final RGB new_rgb)
    {
        if (new_rgb.equals(rgb))
            return;
        rgb = new_rgb;
        fireItemLookChanged();
    }

    /** @return Line width */
    public int getLineWidth()
    {
        return line_width;
    }

    /** @param width New line width */
    public void setLineWidth(int width)
    {
        if (width < 0)
            width = 0;
        if (width == this.line_width)
            return;
        line_width = width;
        fireItemLookChanged();
    }

    /** @return {@link TraceType} for displaying the trace */
    public TraceType getTraceType()
    {
        return trace_type;
    }

    /** @param trace_type New {@link TraceType} for displaying the trace */
    public void setTraceType(final TraceType trace_type)
    {
        if (this.trace_type == trace_type)
            return;
        this.trace_type = trace_type;
        fireItemLookChanged();
    }

    /** @return Y-Axis */
    public AxisConfig getAxis()
    {
        return axis;
    }

    /** @return Index of Y-Axis in model */
    public int getAxisIndex()
    {   // Allow this to work in Tests w/o model
        if (model.isPresent())
            return model.get().getAxisIndex(axis);
        else
            return 0;
    }

    /** @param axis New X-Axis index */
    public void setAxis(final AxisConfig axis)
    {   // Comparing exact AxisConfig reference, not equals()!
    	if (axis == this.axis)
            return;
        this.axis = axis;
        fireItemLookChanged();
    }

    /** This method should be overridden if the instance needs
     *  to change its behavior according to waveform index.
     *  If it is not overridden, this method always return 0.
     *  @return Waveform index
     */
    public int getWaveformIndex()
    {
        return 0;
    }

    /** This method should be overridden if the instance needs
     *  to change its behavior according to waveform index.
     *  @param index New waveform index
     */
    public void setWaveformIndex(int index)
    {
    	// Do nothing.
    }

    /** @return Samples held by this item */
    abstract public PlotSamples getSamples();

    /** @param selected_sample Sample that is currently selected, for example via cursor */
    public void setSelectedSample(final Optional<PlotDataItem<Instant>> selected_sample)
    {
        this.selected_sample = selected_sample;
    }

    /** @return Sample that is currently selected, for example via cursor */
    public Optional<PlotDataItem<Instant>> getSelectedSample()
    {
        return selected_sample;
    }

    /** Write XML formatted item configuration
     *  @param writer PrintWriter
     */
    abstract public void write(final PrintWriter writer);

    /** Write XML configuration common to all Model Items
     *  @param writer PrintWriter
     */
    protected void writeCommonConfig(final PrintWriter writer)
    {
        XMLWriter.XML(writer, 3, XMLPersistence.TAG_DISPLAYNAME, getDisplayName());
        XMLWriter.XML(writer, 3, XMLPersistence.TAG_VISIBLE, Boolean.toString(isVisible()));
    	XMLWriter.XML(writer, 3, XMLPersistence.TAG_NAME, getName());
        XMLWriter.XML(writer, 3, XMLPersistence.TAG_AXIS, getAxisIndex());
        XMLWriter.XML(writer, 3, XMLPersistence.TAG_LINEWIDTH, getLineWidth());
        XMLPersistence.writeColor(writer, 3, XMLPersistence.TAG_COLOR, getColor());
        XMLWriter.XML(writer, 3, XMLPersistence.TAG_TRACE_TYPE, getTraceType().name());
        XMLWriter.XML(writer, 3, XMLPersistence.TAG_WAVEFORM_INDEX, getWaveformIndex());
    }

    /** Load common XML configuration elements into this item
     *  @param model Model to which this item will belong (but doesn't, yet)
     *  @param node XML document node for this item
     */
    protected void configureFromDocument(final Model model, final Element node)
    {
        display_name = DOMHelper.getSubelementString(node, XMLPersistence.TAG_DISPLAYNAME, display_name);
        visible = DOMHelper.getSubelementBoolean(node, XMLPersistence.TAG_VISIBLE, true);
        // Ideally, configuration should define all axes before they're used,
        // but as a fall-back create missing axes
        final int axis_index = DOMHelper.getSubelementInt(node, XMLPersistence.TAG_AXIS, 0);
        while (model.getAxisCount() <= axis_index)
            model.addAxis();
        axis = model.getAxis(axis_index);
        line_width = DOMHelper.getSubelementInt(node, XMLPersistence.TAG_LINEWIDTH, line_width);
        rgb = XMLPersistence.loadColorFromDocument(node).orElse(null);
        String type = DOMHelper.getSubelementString(node, XMLPersistence.TAG_TRACE_TYPE, TraceType.AREA.name());
        try
        {   // Replace XYGraph types with currently supported types
            if (type.equals("ERROR_BARS"))
                type = TraceType.AREA.name();
            else if (type.equals("CROSSES"))
                type = TraceType.XMARKS.name();
            trace_type = TraceType.valueOf(type);
        }
        catch (Throwable ex)
        {
            trace_type = TraceType.AREA;
        }
        setWaveformIndex(DOMHelper.getSubelementInt(node, XMLPersistence.TAG_WAVEFORM_INDEX, 0));
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return name;
    }
}
