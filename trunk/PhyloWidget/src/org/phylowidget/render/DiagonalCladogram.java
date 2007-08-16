package org.phylowidget.render;

import java.util.Collections;
import java.util.List;

import org.phylowidget.tree.RootedTree;
import org.phylowidget.ui.PhyloNode;

import processing.core.PApplet;

public class DiagonalCladogram extends Cladogram
{

	public DiagonalCladogram(PApplet p)
	{
		super(p);
		DiagonalCladogram.dotMult = 0.25f;
	}

	protected void setOptions()
	{
		keepAspectRatio = true;
		useBranchLengths = false;
	}

	protected float branchPositions(PhyloNode n)
	{
		if (tree.isLeaf(n))
			// If N is a leaf, then it's already been laid out.
			return 0;
		/*
		 * Do the children first.
		 */
		List children = tree.childrenOf(n);
		for (int i = 0; i < children.size(); i++)
		{
			PhyloNode child = (PhyloNode) children.get(i);
			branchPositions(child);
		}
		Collections.sort(children);
		/*
		 * Now, let's put on our thinking caps and try to lay ourselves out
		 * correctly.
		 */
		PhyloNode loChild = (PhyloNode) Collections.min(children);
		PhyloNode hiChild = (PhyloNode) Collections.max(children);
		/*
		 * Find the max depth of each child, and project where the "lower" child
		 * would be in the y axis if it were at that higher depth.
		 */
		float stepSize = 1f / (leaves.size());
		float loLeaves = tree.numEnclosedLeaves(loChild);
		float hiLeaves = tree.numEnclosedLeaves(hiChild);
		float mLeaves = Math.max(loLeaves, hiLeaves);
		// System.out.println("md:" + mLeaves);
		float loChildNewY = loChild.getTargetY() + (mLeaves - loLeaves)
				* stepSize / 2;
		float hiChildNewY = hiChild.getTargetY() - (mLeaves - hiLeaves)
				* stepSize / 2;
		float unscaledY = (loChildNewY + hiChildNewY) / 2;
		float unscaledX = nodeXPosition(n);
		n.setUnscaledPosition(unscaledX, unscaledY);
		return 0;
	}

	protected float nodeXPosition(PhyloNode n)
	{
		return xPosForNumEnclosedLeaves(tree.numEnclosedLeaves(n));
	}

	float xPosForNumEnclosedLeaves(int numLeaves)
	{
		return 1 - (float) (numLeaves - 1) / (float) leaves.size();
	}

	protected void doTheLayout()
	{
		super.doTheLayout();
		numCols = numRows / 2;
	}

	protected void drawLine(PhyloNode n)
	{
		if (tree.parentOf(n) != null)
		{
			PhyloNode parent = (PhyloNode) tree.parentOf(n);
			List list = tree.childrenOf(parent);
			Collections.sort(list);
			int index = list.indexOf(n);
			if (index != 0 && index != list.size() - 1)
			{
				/*
				 * This block is only seen by nodes that are "stuck in the
				 * middle" of a polytomy.Maybe we should we do something a la:
				 * 
				 * http://www.slipperorchids.info/taxonomy/cladogram.jpg
				 * 
				 * I tried this already, but such solutions don't tend to scale
				 * up well with large polytomies.
				 */
			}
			float retreatX = getRetreat();
			float retreatY = getRetreat();
			if (parent.y > n.y)
				retreatY = -retreatY;
			canvas.line(n.x, n.y, parent.x + retreatX, parent.y + retreatY);
			// canvas.line(n.x - rad, n.y, parent.x, n.y);
			// float retreat = 0;
			// if (n.y < parent.y)
			// retreat = -rad;
			// else
			// retreat = rad;
			// canvas.line(parent.x, n.y, parent.x, parent.y + retreat);
		}
	}

	float sqrt2 = (float) Math.sqrt(2);

	protected float getRetreat()
	{
		return getNodeRadius() * sqrt2/2;
	}
}
