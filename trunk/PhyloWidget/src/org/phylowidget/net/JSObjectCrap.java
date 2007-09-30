package org.phylowidget.net;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;

import org.phylowidget.PhyloWidget;

public class JSObjectCrap
{

	public static boolean reflectionWorking = true;
	
	public static void reflectJS(String command,String arg)
	{
		if (!reflectionWorking) return;
		String jsresult = null;
		boolean success = false;
		try
		{
			Method getw = null, eval = null, call = null;
			Object jso = null;
			Class c = Class.forName("netscape.javascript.JSObject");
			Method ms[] = c.getMethods();
			for (int i = 0; i < ms.length; i++)
			{
				if (ms[i].getName().compareTo("getWindow") == 0)
					getw = ms[i];
				else if (ms[i].getName().compareTo("eval") == 0)
					eval = ms[i];
				else if (ms[i].getName().compareTo("call") == 0)
					call = ms[i];
			}
			Object a[] = new Object[1];
			a[0] = PhyloWidget.p; /* this is the applet */
			jso = getw.invoke(c, a); /* this yields the JSObject */
			Object result = call.invoke(jso, new Object[] {command,new String[] {arg}});
//			a[0] = command;
//			Object result = eval.invoke(jso, a);
//			System.out.println(result);
		} catch (InvocationTargetException ite)
		{
			reflectionWorking = false;
		} catch (Exception e)
		{
			reflectionWorking = false;
		}
	}
}
