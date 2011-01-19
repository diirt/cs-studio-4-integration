/* 
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchroton, 
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
package org.csstudio.config.authorizeid;

import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.csstudio.config.authorizeid.AuthorizeIdTableViewerFactory.AuthorizeIdTableColumns;
import org.csstudio.platform.security.RegisteredAuthorizationId;
import org.csstudio.platform.security.SecurityFacade;
import org.csstudio.platform.ui.util.CustomMediaFactory;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Defines cell contents for the authorize id table (the topmost one).
 * If changes are made, make sure it corresponds to the column layout as defined in the viewer factory.
 * 
 * @author jpenning
 * @since 19.01.2011
 */
class AuthorizeIdLabelProvider extends LabelProvider implements ITableLabelProvider {
    
    // Icons are used for the boolean column
    private static final Image CHECKED = CustomMediaFactory.getInstance()
            .getImageFromPlugin(AuthorizeIdActivator.PLUGIN_ID, "/res/icons/enabled_co.gif");
    private static final Image UNCHECKED = CustomMediaFactory.getInstance()
            .getImageFromPlugin(AuthorizeIdActivator.PLUGIN_ID, "/res/icons/disabled_co.gif");
    
    @Override
    @CheckForNull
    public Image getColumnImage(@Nullable Object element, int columnIndex) {
        Image result = null;
        
        AuthorizeIdTableColumns colIndex = AuthorizeIdTableColumns.values()[columnIndex];
        
        if (colIndex == AuthorizeIdTableColumns.REGISTERED_AS_EXTENSION) {
            result = getRegisteredAuthorizationId(element.toString()) == null ? UNCHECKED : CHECKED;
        }
        
        return result;
    }
    
    @Override
    @CheckForNull
    public String getColumnText(@Nonnull Object element, int columnIndex) {
        String result = null;
        String authorizationId = element.toString();
        
        AuthorizeIdTableColumns colIndex = AuthorizeIdTableColumns.values()[columnIndex];
        switch (colIndex) {
            case AUTH_ID:
                result = authorizationId;
                break;
            case DESCRIPTION:
                result = getDescription(authorizationId);
                break;
            case REGISTERED_AS_EXTENSION:
                // no string here, see getColumnImage
                break;
        }
        return result;
    }
    
    @CheckForNull
    private RegisteredAuthorizationId getRegisteredAuthorizationId(@Nonnull final String authorizationId) {
        RegisteredAuthorizationId result = null;
        Collection<RegisteredAuthorizationId> authIds = SecurityFacade.getInstance()
                .getRegisteredAuthorizationIds();
        for (RegisteredAuthorizationId registeredAuthorizationId : authIds) {
            if (registeredAuthorizationId.getId().equals(authorizationId)) {
                result = registeredAuthorizationId;
                break;
            }
        }
        return result;
    }
    
    @CheckForNull
    private String getDescription(@Nonnull final String authorizationId) {
        RegisteredAuthorizationId registeredAuthorizationId = getRegisteredAuthorizationId(authorizationId);
        return registeredAuthorizationId == null ? null : registeredAuthorizationId
                .getDescription();
    }
    
}