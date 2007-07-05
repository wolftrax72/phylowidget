package org.andrewberman.ui;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public interface UIObject
{

	public void mouseEvent(MouseEvent e);
	public void keyEvent(KeyEvent e);
	
}