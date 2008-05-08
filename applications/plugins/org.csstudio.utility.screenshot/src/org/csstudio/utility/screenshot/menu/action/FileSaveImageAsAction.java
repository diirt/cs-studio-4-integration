
/* 
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron, 
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

package org.csstudio.utility.screenshot.menu.action;

import org.csstudio.utility.screenshot.ScreenshotWorker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.FileDialog;

public class FileSaveImageAsAction extends Action
{
    private ScreenshotWorker worker   = null;
    private String[] ext = new String[] { "*.bmp", "*.jpg" };
    private String[] filter = new String[] { "Windows Bitmap (*.bmp)", "JPEG (*.jpg)" };  
    private int[] type = new int[] { SWT.IMAGE_BMP, SWT.IMAGE_JPEG };
    
    /**
     * @author Markus M�ller
     * @param w
     */
    
    public FileSaveImageAsAction(ScreenshotWorker w)
    {
        worker = w;
        
        this.setText("Save Image as ...");

        this.setToolTipText("Save the image.");
        
        this.setEnabled(true);
    }
    
    public void run()
    {
        int indexOfDot = -1;
        int indexExt = -1;
        
        FileDialog dialog = new FileDialog(worker.getDisplay().getActiveShell(), SWT.SAVE);
        
        dialog.setFilterExtensions(ext);
        dialog.setFilterNames(filter);
        
        String result = dialog.open();
        
        if(result != null)
        {
            indexOfDot = result.lastIndexOf('.');
            
            if(indexOfDot != -1)
            {
                String e = result.substring(indexOfDot + 1).toLowerCase();
                
                for(int i = 0;i < ext.length;i++)
                {
                    if(ext[i].indexOf(e) != -1)
                    {
                        indexExt = i;
                        
                        break;
                    }
                }
                
                if(indexExt != -1)
                {
                    System.out.println(" Try to save image now...");
                    
                    ImageLoader loader = new ImageLoader();
                    
                    ImageData[] imageData = new ImageData[1];
                    
                    if(worker.getDisplayedImage() != null)
                    {
                        System.out.println(" Saving Displayed image");
                        imageData[0] = worker.getDisplayedImage().getImageData();
                    }
                    else if(worker.getSimpleImage() != null)
                    {
                        System.out.println(" Saving Simple image");
                        imageData[0] = worker.getSimpleImage().getImageData();
                    }
                    else
                    {
                        System.out.println(" NO IMAGE!!!!");
                    }
                    
                    loader.data = imageData;
             
                    try
                    {
                        loader.save(result, type[indexExt]);
                    }
                    catch(SWTException swte)
                    {
                        MessageDialog.openError(worker.getDisplay().getActiveShell(), worker.getNameAndVersion(), "*** SWTException *** : " + swte.getMessage());
                    }
                }
                else
                {
                    MessageDialog.openError(worker.getDisplay().getActiveShell(), worker.getNameAndVersion(), "Unsupported file type: " + e);
                }
            }
        }
    }
}
