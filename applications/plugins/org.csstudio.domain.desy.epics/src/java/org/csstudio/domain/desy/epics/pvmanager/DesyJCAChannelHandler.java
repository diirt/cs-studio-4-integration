/*
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchrotron,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.domain.desy.epics.pvmanager;

import gov.aps.jca.Channel;
import gov.aps.jca.Context;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.STS;
import gov.aps.jca.event.MonitorEvent;

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.csstudio.domain.desy.epics.time.DesyDbrTimeValidator;
import org.csstudio.domain.desy.epics.types.EpicsMetaData;
import org.csstudio.domain.desy.epics.types.EpicsSystemVariable;
import org.csstudio.domain.desy.typesupport.BaseTypeConversionSupport;
import org.csstudio.domain.desy.typesupport.TypeSupportException;
import org.epics.pvmanager.ValueCache;
import org.epics.pvmanager.jca.JCAChannelHandler;
import org.epics.pvmanager.jca.TypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

/**
 * Dedicated DESY channel handler providing the DESY type factories and
 * value update behaviour.
 *
 * @author bknerr
 * @since 30.08.2011
 */
public class DesyJCAChannelHandler extends JCAChannelHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DesyJCAChannelHandler.class);
    private static final Logger STRANGE_LOG = LoggerFactory.getLogger("StrangeThingsLogger");

    private final Predicate<DBR> _validator;
    private Class<?> _dataType;
    private EpicsMetaData _desyMeta;

    /**
     * Constructor.
     * @param dataType
     * @throws TypeSupportException
     */
    public DesyJCAChannelHandler(@Nonnull final String channelName,
                                 @CheckForNull final String dataType,
                                 @Nullable final Context context,
                                 final int monitorMask) {
        super(channelName, context, monitorMask);
        _validator = new DesyDbrTimeValidator();

        if (dataType == null) {
            _dataType = null;
        } else {
            try {
                _dataType = BaseTypeConversionSupport.createBaseTypeClassFromString(dataType, "org.csstudio.domain.desy.epics.types");
            } catch (final TypeSupportException e) {
                LOG.error("Datatype for channel {} is not convertible to java type class!:\n{}", channelName, e.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    @Override
    @Nonnull
    protected TypeFactory matchFactoryFor(@Nonnull final Class<?> desiredType,
                                          @Nonnull final Channel channel) {
        return DesyTypeFactoryProvider.matchFor(channel);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public boolean updateCache(@Nonnull final MonitorEvent event,
                               @Nonnull final ValueCache<?> cache) {
        final DBR rawDBR = event.getDBR();

        handleFirstCacheUpdate(rawDBR);

        if (!_validator.apply(rawDBR)) {
            STRANGE_LOG.info("{} has invalid timestamp.", getChannelName());
            return false;
        }

        @SuppressWarnings("unchecked")
        final EpicsSystemVariable newValue =
            ((DesyJCATypeFactory) vTypeFactory).createValue(getChannelName(),
                                                            rawDBR,
                                                            metadata,
                                                            _desyMeta);
        cache.setValue(newValue);
        return true;
    }

    @SuppressWarnings("rawtypes")
    private void handleFirstCacheUpdate(@Nonnull final DBR rawDBR) {
        if (_desyMeta == null) {
            _desyMeta = EpicsMetaData.EMPTY_DATA;
            if (metadata != null) {
                _desyMeta = ((DesyJCATypeFactory) vTypeFactory).createMetaData((STS) metadata);
            }
            if (_dataType != null) {
                ((DesyJCATypeFactory) vTypeFactory).setIsArray(Collection.class.isAssignableFrom(_dataType));
            } else {
                ((DesyJCATypeFactory) vTypeFactory).setIsArray(rawDBR.getCount() > 1);
            }
        }
    }
}
