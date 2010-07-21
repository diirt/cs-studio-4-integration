package org.csstudio.swt.widgets.figures;
import org.eclipse.draw2d.Figure;


public class LabelFigureTest extends AbstractWidgetTest{

	@Override
	public Figure createTestWidget() {
		return new LabelFigure();
	}
	
	
	@Override
	public String[] getPropertyNames() {
		String[] superProps =  super.getPropertyNames();
		String[] myProps = new String[]{
				"horizontalAlignment",
				"text",
				"verticalAlignment",
				"runMode",
				"selectable"
		};
		
		return concatenateStringArrays(superProps, myProps);
	}
	
	@Override
	public boolean isAutoTest() {
		return true;
	}		
}
