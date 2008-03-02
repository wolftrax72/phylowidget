/*******************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of PhyloWidget.
 * 
 * PhyloWidget is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * PhyloWidget is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * PhyloWidget. If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.ui.menu;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Stack;

import org.andrewberman.ui.tools.Tool;
import org.phylowidget.PhyloWidget;

import processing.core.PApplet;
import processing.xml.XMLElement;

public class MenuIO
{
	static PApplet app;
	static Object actionObject;

	/**
	 * 
	 * @param p
	 * @param filename
	 * @param actionHolder
	 *            (optional) the object which will contain the action methods
	 *            for this menu set.
	 * @return
	 */
	public static ArrayList loadFromXML(PApplet p, String filename,
			Object actionHolder)
	{
		ArrayList menus = new ArrayList();
		app = p;
		actionObject = actionHolder;
		InputStream in = p.openStream(filename);
		/*
		 * Search depth-first through the XML tree, adding the highest-level
		 * menu elements we can find.
		 */
		Stack s = new Stack();
		try
		{
			s.push(new XMLElement(in));
			while (!s.isEmpty())
			{
				XMLElement curEl = (XMLElement) s.pop();
				if (curEl.getName().equalsIgnoreCase("menu"))
				{
					// If curEl is a menu, parse it and add it to the ArrayList.
					menus.add(processElement(null, curEl));
				} else
				{
					// If not, keep going through the XML tree and search for
					// more <menu> elements.
					Enumeration en = curEl.enumerateChildren();
					while (en.hasMoreElements())
					{
						s.push(en.nextElement());
					}
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return menus;
	}

	public static MenuItem processElement(MenuItem parent, XMLElement el)
	{
		MenuItem newItem = null;
		String elName = el.getName();
		String itemName = el.getStringAttribute("name");
		el.removeAttribute("name");
		if (el.hasAttribute("type"))
		{
			/*
			 * If this element has the "type" attribute, then we use that to
			 * create a new Menu or MenuItem from scratch.
			 */
			String type = el.getStringAttribute("type");
			el.removeAttribute("type");
			newItem = createMenu(type);
			// Set this Menu's name.
			if (itemName != null)
				newItem.setName(itemName);
			else
				newItem.setName("");
		}

		/*
		 * If this is any other element (I expect it to be <item>), then
		 * let's make sure it has a parent Menu or MenuItem:
		 */
		if (parent == null && !elName.equalsIgnoreCase("menu"))
			throw new RuntimeException("[MenuIO] XML menu parsing error on "
					+ elName
					+ " element: <item> requires a parent <menu> or <item>!");

		if (elName.equalsIgnoreCase("item"))
		{
			/*
			 * If all is well, then we use the parent item's add() method to
			 * create this new Item element.
			 */
			if (newItem != null)
				newItem = parent.add(newItem);
			else
				newItem = parent.add(itemName);
		} else if (elName.equalsIgnoreCase("methodcall"))
		{
			String mName = el.getStringAttribute("method");
			String p = el.getStringAttribute("param");
			el.removeAttribute("method");
			el.removeAttribute("param");
			try
			{
				Method m = parent.getClass().getMethod(mName,
						new Class[] { String.class });
				m.invoke(parent, new Object[] { p });
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		/*
		 * At this point, we have a good "newItem" MenuItem. Now we need to
		 * populate its attributes using a bean-like Reflection scheme. Every
		 */
		Enumeration attrs = el.enumerateAttributeNames();
		while (attrs.hasMoreElements())
		{
			String attr = (String) attrs.nextElement();
			/*
			 * Skip the attributes that we already used to do something with.
			 */
			//			if (attr.equalsIgnoreCase("name") || 
			//					attr.equalsIgnoreCase("type") ||
			//					attr.equalsIgnoreCase("method") ||
			//					attr.equalsIgnoreCase("param"))
			//				continue;
			/*
			 * For all other attributes, call the set[Attribute] method of the
			 * MenuItem.
			 */
			setAttribute(newItem, attr, el.getStringAttribute(attr));
		}

		/*
		 * Now, keep the recursion going: go through the current XMLElement's
		 * children and call menuElement() on each one.
		 */
		XMLElement[] els = el.getChildren();
		for (int i = 0; i < els.length; i++)
		{
			XMLElement child = els[i];
			processElement(newItem, child);
		}
		return newItem;
	}

	static final String menuPackage = Menu.class.getPackage().getName();
	static final String toolPackage = Tool.class.getPackage().getName();

	/**
	 * Uses Reflection to create a Menu of the given class type.
	 * 
	 * @param classType
	 *            The desired Menu class to create, either as a simple class
	 *            name (if the class resides within the base Menu package) or as
	 *            the fully-qualified Class name (i.e.
	 *            org.something.SomethingElse).
	 * @return
	 */
	private static MenuItem createMenu(String classType)
	{
		/*
		 * We need to give the complete package name of the desired Class, so we
		 * need to assume that the desired class resides within the base Menu
		 * package.
		 */
		String fullClass = menuPackage + "." + classType;
		Class c = null;
		try
		{
			c = Class.forName(fullClass);
		} catch (java.lang.ClassNotFoundException e1)
		{

			try
			{
				c = Class.forName(classType);
			} catch (java.lang.ClassNotFoundException e2)
			{
				e2.printStackTrace();
			}
		}

		Constructor construct;
		try
		{
			construct = c.getConstructor(new Class[] { PApplet.class });
			Object newMenu = construct.newInstance(new Object[] { app });
			return (Menu) newMenu;
		} catch (Exception e)
		{
			//			 e.printStackTrace();
			try
			{
				construct = c.getConstructor(new Class[] {});
				Object newMenu = construct.newInstance(new Object[] {});
				return (MenuItem) newMenu;
			} catch (Exception e2)
			{
				e2.printStackTrace();
				return null;
			}
		}
	}

	/**
	 * Use reflection to call the setXXX function for the given attribute and
	 * value.
	 * 
	 * All setXXX methods should just take a String argument, except for the
	 * defined exceptions.
	 * 
	 * @param item
	 * @param attr
	 * @param value
	 */
	private static void setAttribute(MenuItem item, String attr, String value)
	{
		attr = attr.toLowerCase();
		String upperFirst = "set" + upperFirst(attr);
		try
		{
			Class[] argC = null;
			Object[] args = null;
			if (attr.equalsIgnoreCase("action"))
			{
				/*
				 * If this attribute is an Action, then we need to include a
				 * reference to our actionObject so the correct method can be
				 * called by this menuItem's action.
				 */
				argC = new Class[] { Object.class, String.class };
				args = new Object[] { actionObject, value };
			} else if (attr.equalsIgnoreCase("tool"))
			{
				/*
				 * If this attribute is a Tool, then we need to set the first
				 * letter to upper-case.
				 */
				argC = new Class[] { String.class };
				args = new Object[] { upperFirst(value) };
			} else if (attr.equalsIgnoreCase("property"))
			{
				/*
				 * If this is a setProperty command, include a reference to the
				 * actionObject.
				 */
				argC = new Class[] { Object.class, String.class };
				args = new Object[] { actionObject, value };
			} else
			{
				/*
				 * EVERYTHING ELSE: we simply call the setXXX(value) method
				 * using Java's reflection API.
				 */
				argC = new Class[] { String.class };
				args = new Object[] { value };
			}
			Method curMethod = null;
			try
			{
				/*
				 * First, try it with the straight String parameter.
				 */
				Method[] methods = item.getClass().getMethods();
				for (int i = 0; i < methods.length; i++)
				{
					if (methods[i].getName().equalsIgnoreCase(upperFirst))
					{
						curMethod = methods[i];
						curMethod.invoke(item, args);
						break;
					}
				}
			} catch (Exception e)
			{
				/*
				 * If the String didn't work, try parsing the String to a float.
				 */
				curMethod.invoke(item, new Object[] { new Float(value) });
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static String upperFirst(String s)
	{
		String upper = s.substring(0, 1).toUpperCase();
		String orig = s.substring(1, s.length());
		return upper + orig;
	}
}