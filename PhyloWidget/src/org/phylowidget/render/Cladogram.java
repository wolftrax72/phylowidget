package org.phylowidget.render;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;

import org.andrewberman.ui.Point;
import org.andrewberman.ui.TextField;
import org.andrewberman.ui.UIUtils;
import org.phylowidget.PhyloWidget;
import org.phylowidget.tree.RenderNode;
import org.phylowidget.tree.TreeNode;

import processing.core.PApplet;
import processing.core.PGraphicsJava2D;

public class Cladogram extends TreeRenderer.Abstract
{
	/**
	 * Another optimization method -- if we're zoomed out so far that rows
	 * become unreadable (i.e. if rowsize is <= SKIP_THRESH), then start
	 * skipping drawing nodes to relieve the strain on the text functions.
	 */
	HashMap skipMe = new HashMap();
	public static final int SKIP_THRESH = 3;
	/**
	 * If true, this tree will maintain its "proper" aspect ratio, meaning it
	 * won't stretch to completely fill its enclosing rectangle.
	 */
	public boolean keepAspectRatio = true;
	/**
	 * Fontmetrics for calculating text widths.
	 */
	FontMetrics fm;
	/**
	 * These variables are set in the calculateSizes() method during every round
	 * of rendering. Very important!
	 */
	protected float rowSize, colSize, numRows, numCols;

	/**
	 * Rotation of the text, in radians.
	 */
	public float textRotation = 0;//PApplet.PI/10f;
	/**
	 * Size of the text, as a multiplier relative to normal size.
	 */
	public float textSize = 1f;
	
	protected float dFont;

	protected int maxDepth;
	/**
	 * Radius of the node ellipses.
	 */
	protected float dotWidth, rad;
	/**
	 * Width of the node label gutter.
	 */
	protected float gutterWidth = 0;
	protected String biggestString;

	protected Point ptemp = new Point(0, 0);
	protected Point ptemp2 = new Point(0, 0);

	public static float dotMult = 0.5f;
	
	public Cladogram(PApplet p)
	{
		super(p);
	}

	protected void doTheLayout()
	{
		/*
		 * ASSUMPTION: the leaves ArrayList contains a "sorted" view of the
		 * tree's leaves, i.e. in the correct ordering from top to bottom.
		 */
		maxDepth = tree.getRoot().getMaxDepth();
		gutterWidth = 0;
		biggestString = "";
		// float spaceWidth = font.width(' ') * 3;
		if (UIUtils.isJava2D(canvas))
			fm = UIUtils.getMetrics(canvas, font.font, 100);
		/*
		 * Set the leaf positions.
		 */
		for (int i = 0; i < leaves.size(); i++)
		{
			RenderNode n = (RenderNode) leaves.get(i);
			/*
			 * Set the leaf position of this node.
			 */
			leafPosition(n, i);
			/**
			 * Find the width of this node's label.
			 */
			float width = 0;// spaceWidth;
			if (UIUtils.isJava2D(canvas))
			{
//				width = fm.stringWidth(n.getName());
				width = (float) fm.getStringBounds(n.getName(), p.getGraphics()).getWidth() / 100f;
			} else
			{
				char[] chars = n.getName().toCharArray();

				for (int j = 0; j < chars.length; j++)
				{
					width += font.width(chars[j]);
				}
			}
			n.unitTextWidth = width;
			if (width > gutterWidth)
			{
				gutterWidth = width;
				biggestString = n.getName();
			}
		}
		/*
		 * Special case: if biggestString is 0 length, we'll fudge it.
		 */
		if (biggestString.length() == 0)
		{
			biggestString = "P";
			gutterWidth = fm.stringWidth(biggestString)/100f;
		}
		/*
		 * Now, set the branch positions.
		 */
		branchPositions((RenderNode) tree.getRoot());
		/*
		 * Set the numRows and numCols variables.
		 */
		numRows = leaves.size();
		numCols = maxDepth;
	}

	void leafPosition(RenderNode n, int index)
	{
		/**
		 * Set the leaf position.
		 */
		float yPos = ((float)(index+.5f) / (float)(leaves.size()));
		float xPos = 1;
		n.unscaledX = xPos;
		n.unscaledY = yPos;
	}

	/**
	 * A recursive function to set the positions for all the branches. Only
	 * works if the leaf positions have already been set.
	 * 
	 * @param n
	 * @return
	 */
	protected float branchPositions(RenderNode n)
	{
		if (n.isLeaf())
		{
			// If N is a leaf, then it's already been laid out.
			return n.unscaledY;
		} else
		{
			// If not:
			// Y coordinate should be the average of its children's heights
			ArrayList children = n.getChildren();
			float sum = 0;
			for (int i = 0; i < children.size(); i++)
			{
				RenderNode child = (RenderNode) children.get(i);
				sum += branchPositions(child);
			}
			float y = (float) sum / (float) children.size();
			float x = 1 - (float)(n.getMaxDepth()) / (float)maxDepth;
			n.unscaledX = x;
			n.unscaledY = y;
			return y;
		}
	}

	boolean forward;
	protected void drawRecalc()
	{
//		if (forward)
//			textRotation += 0.02f;
//		else
//			textRotation -= 0.02f;
//		if (textRotation < -PApplet.HALF_PI)
//			forward = true;
//		if (textRotation > PApplet.HALF_PI)
//			forward = false;

		/*
		 * Figure out the ideal row size.
		 */
		float overhang = gutterWidth * (float)Math.sin(textRotation);
		float absOverhang = Math.abs(overhang);
		rowSize = rect.height / (numRows + absOverhang);
		textSize = Math.min(rect.width / gutterWidth * .5f, rowSize);
		/*
		 * Using that, figure out the ideal column size.
		 */
//		if (keepAspectRatio)
			colSize = rect.width / (numCols + 1 + gutterWidth);
//		else
//			colSize = (rect.width - textSize*(1+gutterWidth)) / numCols;
		/*
		 * Now let us constrain the aspect ratio.
		 */
		if (keepAspectRatio)
			constrainAspectRatio();
		textSize = Math.min(rowSize,textSize);
		dotWidth = textSize * dotMult;
		rad = dotWidth / 2;
		if (numRows == 1)
			scaleX = 0;
		else
			scaleX = colSize * (numCols);
		scaleY = rowSize * numRows;
		dx = (rect.width - scaleX - gutterWidth*textSize - textSize) / 2;
		dy = (rect.height - scaleY - overhang*textSize) / 2;
		dx += rect.getX();
		dy += rect.getY();
		dFont = (font.ascent() - font.descent()) * textSize / 2;
		/*
		 * Update all the node ranges.
		 */
		updateNodeRanges();
	}
	
	protected void constrainAspectRatio()
	{
		rowSize = colSize = Math.min(rowSize,colSize);
	}
	
	protected void updateNodeRanges()
	{
		/*
		 * Update all the XYRange objects.
		 */
		for (int i = 0; i < ranges.size(); i++)
		{
			NodeRange r = (NodeRange) ranges.get(i);
			RenderNode n = r.node;
			n.x = n.unscaledX * scaleX + dx;
			n.y = n.unscaledY * scaleY + dy;
			RenderNode parent;
			if (n.getParent() != TreeNode.NULL_PARENT)
				parent = (RenderNode) n.getParent();
			else
			{
				parent = n;
			}
			switch (r.type)
			{
				case (Abstract.NODE):
					r.loX = Math.min(n.x - rad, parent.x - rad);
					r.hiX = Math.max(n.x + rad, parent.x + rad);
					r.loY = Math.min(n.y - rad, parent.y - rad);
					r.hiY = Math.max(n.y + rad, parent.y + rad);
					break;
				case (Abstract.LABEL):
					r.loX = n.x + dotWidth;
					float textHeight = (font.ascent() + font.descent())
							* textSize;
					r.loY = n.y - textHeight / 2;
					r.hiY = n.y + textHeight / 2;
					float width = 0;
					width = n.unitTextWidth * textSize;
					r.hiX = r.loX + width;
					break;
			}
		}
		list.sort();
		// skipNodes();
	}

	protected void skipNodes()
	{
		/*
		 * Skip nodes if the rows are small enough to allow it.
		 */
		skipMe.clear();
		if (rowSize < SKIP_THRESH)
		{
			for (int i = 0; i < leaves.size(); i++)
			{
				RenderNode n = (RenderNode) leaves.get(i);
				int asdf = 1;
				if (rowSize < SKIP_THRESH)
					asdf = 2;
				if (rowSize < SKIP_THRESH * .5)
					asdf = 3;
				if (rowSize < SKIP_THRESH * .25)
					asdf = 4;
				if (rowSize < SKIP_THRESH * .1)
					asdf = 10;
				if (i % asdf != 0)
					skipMe.put(n, null);
			}
		}
	}

	protected void drawNode(RenderNode n)
	{
		canvas.ellipse(n.x, n.y, dotWidth, dotWidth);
	}

	protected void drawLine(RenderNode n)
	{
		if (n.getParent() != TreeNode.NULL_PARENT)
		{
			RenderNode parent = (RenderNode) n.getParent();
			canvas.line(n.x - rad, n.y, parent.x, n.y);
			float retreat = 0;
			if (n.y < parent.y)
				retreat = -rad;
			else
				retreat = rad;
			canvas.line(parent.x, n.y, parent.x, parent.y + retreat);
		}
	}

	protected void drawLabel(RenderNode n)
	{
			p.pushMatrix();
			p.translate(n.x+dotWidth, n.y);
			p.rotate(textRotation);
		if (PhyloWidget.usingNativeFonts)
		{
			PGraphicsJava2D pgj = (PGraphicsJava2D) canvas;
			Graphics2D g2 = pgj.g2;
			g2.setFont(font.font.deriveFont(textSize));
			g2.setPaint(Color.black);
			g2.drawString(n.getName(), 0, 0+ dFont);
		} else
			canvas.text(n.getName(), n.x + dotWidth, n.y + dFont);
			p.popMatrix();
	}

	public float getNodeRadius()
	{
		return dotWidth / 2;
	}

	public void positionText(RenderNode n, TextField tf)
	{
		tf.setTextSize(textSize);
		float tfWidth = UIUtils.getTextWidth(canvas, font, textSize, tf
				.getText(), true);
		float textWidth = Math.max(n.unitTextWidth * textSize + 5, tfWidth);
		tf.setWidth(textWidth);
		tf.setPositionByBaseline(n.x+dotWidth, n.y + dFont);
	}

}