/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.server;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.csstudio.alarm.beast.Preferences;
import org.csstudio.alarm.beast.SeverityLevel;
import org.csstudio.platform.data.ISeverity;
import org.csstudio.platform.data.ITimestamp;
import org.csstudio.platform.data.IValue;
import org.csstudio.platform.data.TimestampFactory;
import org.csstudio.platform.logging.CentralLogger;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVFactory;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.osgi.util.NLS;

/** A PV with alarm state
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmPV extends AlarmLogic implements PVListener, FilterListener
{
    /** Timer used to check for connections at some delay after 'start */
    final private static Timer connection_timer =
        new Timer("Connection Check", true);
    
    final private Logger log;
    
    /** Name of the alarm
     *  @see #getName()
     */
    final private String name;

    /** Alarm server that handles this PV */
    final private AlarmServer server;
    
    /** RDB ID */
    final private int id;
    
    /** Description of alarm, will be used to annunciation */
    private volatile String description;

    /** Is this a 'priority' message?
     *  @see #isPriorityAlarm()
     */
    private boolean has_priority;

    /** Control system PV */
    final private PV pv;

    /** Started when pv is created to check if it ever connects */
    private TimerTask connection_timeout_task = null;

    /** Filter that might be used to compute 'enabled' state;
     *  can be <code>null</code>
     */
    private volatile Filter filter;

    /** Initialize alarm PV
     *  @param server Alarm server that handles this PV 
     *  @param id RDB ID
     *  @param name Name of control system PV
     *  @param description Description of alarm, will be used to annunciation
     *  @param enabled Enable the alarm logic?
     *  @param latching Latch the highest received alarm severity?
     *  @param annunciating Annunciate alarms?
     *  @param min_alarm_delay Minimum time in alarm before declaring an alarm
     *  @param count Alarm when PV != OK more often than this count within delay
     *  @param filter Filter expression for enablement or <code>null</code>
     *  @param current_severity Current alarm severity
     *  @param current_message Current system message
     *  @param severity Alarm system severity
     *  @param message Alarm system message
     *  @param value Value
     *  @param timestamp 
     *  @throws Exception on error
     */
    public AlarmPV(final AlarmServer server, final int id, final String name, 
            final String description,
            final boolean enabled,
            final boolean latching,
            final boolean annunciating,
            final int min_alarm_delay,
            final int count,
            final String filter,
            final SeverityLevel current_severity,
            final String current_message,
            final SeverityLevel severity,
            final String message,
            final String value,
            final ITimestamp timestamp) throws Exception
    {
        super(latching, annunciating, min_alarm_delay, count,
              new AlarmState(current_severity, current_message, "", timestamp),
              new AlarmState(severity, message, value, timestamp));
        log = CentralLogger.getInstance().getLogger(this);
        this.name = name;
        this.server = server;
        this.id = id;
        setDescription(description);
        pv = PVFactory.createPV(name);
        setEnablement(enabled, filter);
    }

    /** @return RDB ID for this PV */
    public int getID()
    {
        return id;
    }

    /** Get the alarm PV name.
     *  This may differ a little from the actual underlying control system PV:
     *  For example, the alarm name might be "Fred",
     *  but since the control system defaults to "system://Fred", the actual
     *  CS PV has a slightly different name.
     *  @return Name that was used when constructing this AlarmPV
     */
    public String getName()
    {
        return name;
    }

    /** @return Alarm description */
    public String getDescription()
    {
        return description;
    }

    /** @param description Alarm description */
    public void setDescription(final String description)
    {
        this.description = description;
        // Determine 'priority' based on description
        final String basic_description;
        if (description.startsWith(Messages.BasicAnnunciationPrefix))
            basic_description = description.substring(Messages.BasicAnnunciationPrefix.length()).trim();
        else
            basic_description = description.trim();
        has_priority = basic_description.startsWith("!");
    }

    @Override
    public boolean isPriorityAlarm()
    {
        return has_priority;
    }

    /** Set enablement.
     *  <p>
     *  Must not be called on a running PV.
     *  @param enabled Enable state that's used in absence of filter
     *  @param filter New filter expression, may be <code>null</code>
     *  @throws Exception on error
     */
    void setEnablement(final boolean enabled, final String filter) throws Exception
    {
        if (pv.isRunning())
            throw new Exception("Cannot change enablement while running");
        synchronized (this)
        {
            if (filter == null  ||  filter.length() <= 0)
                this.filter = null;
            else
                this.filter = new Filter(filter, this);
            setEnabled(enabled);
        }
    }

    /** Connect to control system */
    public void start() throws Exception
    {
        // Seconds to millisecs
        final long delay = Preferences.getConnectionGracePeriod() * 1000;
        connection_timeout_task = new TimerTask()
        {
            @Override
            public void run()
            {
                if (pv.isRunning() && !pv.isConnected())
                    pvConnectionTimeout();
            }
        };
        connection_timer.schedule(connection_timeout_task, delay);
        pv.addListener(this);
        pv.start();
        if (filter != null)
            filter.start();
    }

    /** Disconnect from control system */
    public void stop()
    {
        if (connection_timeout_task != null)
        {
            connection_timeout_task.cancel();
            connection_timeout_task = null;
        }
    	if (filter != null)
    		filter.stop();
        pv.removeListener(this);
        pv.stop();
    }

    /** @see FilterListener */
    public void filterChanged(final double value)
    {
    	final boolean new_enable_state = value > 0.0;
        if (log.isDebugEnabled())
            log.debug(getName() + " filter " + 
                      (new_enable_state ? "enables" : "disables"));
        setEnabled(new_enable_state);
	}

    /** Invoked by <code>connection_timer</code> when PV fails to connect
     *  after <code>start()</code>
     */
	private void pvConnectionTimeout()
    {
	    
	    final AlarmState received = new AlarmState(SeverityLevel.INVALID,
               Messages.AlarmMessageNotConnected, "", TimestampFactory.now());
	    computeNewState(received);
    }

    /** @see PVListener */
    public void pvDisconnected(final PV pv)
    {
        // Also ignore the disconnect event that can result from stop()
    	if (!pv.isRunning())
    		return;
        final AlarmState received = new AlarmState(SeverityLevel.INVALID,
                Messages.AlarmMessageDisconnected, "", TimestampFactory.now());
        computeNewState(received);
    }
    
    /** @see PVListener */
    public void pvValueUpdate(final PV pv)
    {
    	// Inspect alarm state of received value
        final IValue value = pv.getValue();
        final SeverityLevel new_severity = decodeSeverity(value);
        final String new_message = value.getStatus();
        final AlarmState received = new AlarmState(new_severity, new_message,
                value.format(), value.getTime());
        computeNewState(received);
    }

    /** Decode a value's severity
     *  @param value Value to decode
     *  @return SeverityLevel
     */
    private SeverityLevel decodeSeverity(final IValue value)
    {
        final ISeverity sev = value.getSeverity();
        if (sev.isInvalid())
            return SeverityLevel.INVALID;
        if (sev.isMajor())
            return SeverityLevel.MAJOR;
        if (sev.isMinor())
            return SeverityLevel.MINOR;
        return SeverityLevel.OK;
    }

    /** {@inheritDoc} */
    @Override
    protected void fireEnablementUpdate()
    {
        server.sendEnablementUpdate(this, isEnabled());
    }

    /** {@inheritDoc} */
    @Override
    protected void fireStateUpdates()
    {
        if (log.isDebugEnabled())
            log.debug(getName() + " changes to " + super.toString());
        final AlarmState current, alarm;
        synchronized (this)
        {
            current = getCurrentState();
            alarm = getAlarmState();
        }
        if (server != null)
            server.sendStateUpdate(this,
                    current.getSeverity(), current.getMessage(),
                    alarm.getSeverity(), alarm.getMessage(),
                    alarm.getValue(), alarm.getTime());
    }

    /** {@inheritDoc} */
    @Override
    protected void fireAnnunciation(final SeverityLevel level)
    {
        final String message;
        // For annunciation texts like "* Some Message" where
        // "*" is the BasicAnnunciationPrefix, remove the prefix
        // and use the basic format.
        // Otherwise use the severity level and the text with the
        // normal AnnunciationFmt
        if (description.startsWith(Messages.BasicAnnunciationPrefix))
            message = NLS.bind(Messages.BasicAnnunciationFmt,
                               description.substring(Messages.BasicAnnunciationPrefix.length()));
        else
            message = NLS.bind(Messages.AnnunciationFmt,
                               level.getDisplayName(), description);
        if (server != null)
            server.getTalker().say(level, message);
    }

    /** @return String representation for debugging (server 'dump') */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(getName() + " [" + description + "] - ");
        if (pv.isConnected())
            buf.append("connected - ");
        else
            buf.append("disconnected - ");
        if (! isEnabled())
            buf.append("disabled - ");
        if (isAnnunciating())
            buf.append("annunciating - ");
        if  (isLatching())
            buf.append("latching - ");
        if (getDelay() > 0)
            buf.append(getDelay() + " sec delay - ");
        if (filter != null)
            buf.append(filter.toString() + " - ");
        buf.append(super.toString());
        return buf.toString();
    }
}