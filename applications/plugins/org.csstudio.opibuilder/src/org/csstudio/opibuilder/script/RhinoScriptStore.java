package org.csstudio.opibuilder.script;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.util.ConsoleService;
import org.csstudio.opibuilder.util.ResourceUtil;
import org.csstudio.platform.ui.util.UIBundlingThread;
import org.csstudio.utility.pv.PV;
import org.csstudio.utility.pv.PVListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * The script store help to store the compiled script for the afterward executions. This is 
 * specified for the rhino script engine. The store must be disposed manually when it is not needed.
 * @author Xihui Chen
 *
 */
public class RhinoScriptStore {
	
	private Context scriptContext;
	
	private Scriptable scriptScope;
	
	private Object widgetController;	
	
	private Object pvArrayObject;
	
	private Script script;
	
	private IPath scriptPath;
	
	private String errorSource;
	
	private Map<PV, PVListener> pvListenerMap;
	
	private boolean errorInScript;

	private Map<PV, Boolean> pvConnectStatusMap;	
	
	public RhinoScriptStore(ScriptData scriptData, final AbstractBaseEditPart editpart, 
			final PV[] pvArray) throws Exception {
		errorInScript = false;
		scriptContext = ScriptService.getInstance().getScriptContext();		
		scriptScope = new ImporterTopLevel(scriptContext);	
		
		errorSource = scriptData instanceof RuleScriptData ? 
				((RuleScriptData)scriptData).getRuleData().getName() : scriptData.getPath().toString();
		
		if(scriptData instanceof RuleScriptData){
			script = scriptContext.compileString(((RuleScriptData)scriptData).getScriptString(),
					"rule", 1, null);
		}else{
			scriptPath = scriptData.getPath();
			if(!scriptPath.isAbsolute()){
				scriptPath = ResourceUtil.buildAbsolutePath(
						editpart.getWidgetModel(), scriptPath);
			}
			//read file			
			InputStream inputStream = ResourceUtil.pathToInputStream(scriptPath);
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(inputStream));	
			
			//compile
			script = scriptContext.compileReader(reader, "script", 1, null); //$NON-NLS-1$
			inputStream.close();
			reader.close();
		}
	
		
		widgetController = Context.javaToJS(editpart, scriptScope);
		pvArrayObject = Context.javaToJS(pvArray, scriptScope);
		ScriptableObject.putProperty(scriptScope, ScriptService.WIDGET_CONTROLLER, widgetController);	
		ScriptableObject.putProperty(scriptScope, ScriptService.PV_ARRAY, pvArrayObject);	
		
		
		pvListenerMap = new HashMap<PV, PVListener>();		
		pvConnectStatusMap = new HashMap<PV, Boolean>();
		
		//register pv listener
		int i=0;
		for(PV pv : pvArray){
			if(pv == null || !scriptData.getPVList().get(i++).trigger)
				continue;	
			pvConnectStatusMap.put(pv, true);
			PVListener pvListener = new PVListener() {			
				public void pvValueUpdate(PV pv) {
					//if pv connection is restored from connection
					if(!pvConnectStatusMap.get(pv)){
						ConsoleService.getInstance().writeInfo(
								NLS.bind("Connection to PV {0} restored.", pv.getName()));
						pvConnectStatusMap.put(pv, true);
					}
					
					//execute script only if all input pvs are connected
					if(pvArray.length > 1){
						for(PV pv2 : pvArray){
							if(!pv2.isConnected()){
								String message = NLS.bind(
										"{0} did not executed because the input PV: {1} is disconnected", 
										errorSource, pv2.getName());
								ConsoleService.getInstance().writeWarning(message);
								return;
							}
						}					
					}
					
					UIBundlingThread.getInstance().addRunnable(new Runnable() {						
						public void run() {
							if(!errorInScript){
								try {								
									script.exec(scriptContext, scriptScope);
								} catch (Exception e) {
									errorInScript = true;
									final String message = NLS.bind("Error in {0}.\nAs a consequence, the script or rule will not be executed.\n{1}",
										errorSource, e.getMessage());									
									ConsoleService.getInstance().writeError(message);
								}
							}
						}
					});
				}			
				public void pvDisconnected(PV pv) {
					if(pv.isRunning()){
						pvConnectStatusMap.put(pv, false);
						String message = NLS.bind(
								"The PV: {0} which is one of the inputs of the script or rule: {1} is disconnected.", 
								pv.getName(), errorSource);
						ConsoleService.getInstance().writeWarning(message);
					}
					
				}
			};
			pvListenerMap.put(pv, pvListener);
			pv.addListener(pvListener);
		}
	}
	
	public void dispose() {
		for(PV pv : pvListenerMap.keySet()){
			pv.removeListener(pvListenerMap.get(pv));
		}
		pvListenerMap.clear();
		pvConnectStatusMap.clear();
	}
	
	
	
	
	
}
