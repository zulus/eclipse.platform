/*
 * Created on Nov 26, 2003
 * 
 * To change the template for this generated file go to Window - Preferences -
 * Java - Code Generation - Code and Comments
 */
package org.eclipse.ui.forms.widgets;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
/**
 * Hyperlink is a concrete implementation of the abstract base class that draws
 * text in the client area. Text can be wrapped and underlined. Hyperlink is
 * typically added to the hyperlink group so that certain properties are
 * managed for all the hyperlinks that belong to it.
 * 
 * @see org.eclipse.ui.forms.HyperlinkGroup
 * @since 3.0
 */
public class Hyperlink extends AbstractHyperlink {
	private String text;
	private boolean underlined;
	/**
	 * Creates a new hyperlink control in the provided parent.
	 * 
	 * @param parent
	 *            the control parent
	 * @param style
	 *            the widget style
	 */
	public Hyperlink(Composite parent, int style) {
		super(parent, style);
		initAccessible();
	}
	protected void initAccessible() {
		Accessible accessible = getAccessible();
		accessible.addAccessibleListener(new AccessibleAdapter() {
			public void getName(AccessibleEvent e) {
				e.result = getText();
			}
			public void getHelp(AccessibleEvent e) {
				e.result = getToolTipText();
			}
		});
		accessible.addAccessibleControlListener(new AccessibleControlAdapter() {
			public void getChildAtPoint(AccessibleControlEvent e) {
				Point pt = toControl(new Point(e.x, e.y));
				e.childID = (getBounds().contains(pt))
						? ACC.CHILDID_SELF
						: ACC.CHILDID_NONE;
			}
			public void getLocation(AccessibleControlEvent e) {
				Rectangle location = getBounds();
				Point pt = toDisplay(new Point(location.x, location.y));
				e.x = pt.x;
				e.y = pt.y;
				e.width = location.width;
				e.height = location.height;
			}
			public void getChildCount(AccessibleControlEvent e) {
				e.detail = 0;
			}
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_LABEL;
			}
			public void getState(AccessibleControlEvent e) {
				int state = ACC.STATE_NORMAL;
				if (Hyperlink.this.getSelection())
					state = ACC.STATE_SELECTED | ACC.STATE_FOCUSED;
				e.detail = state;
			}
		});
	}
	/**
	 * Sets the underlined state. It is not necessary to call this method when
	 * in a hyperlink group.
	 * 
	 * @param underlined
	 *            if <samp>true </samp>, a line will be drawn below the text
	 *            for each wrapped line.
	 */
	public void setUnderlined(boolean underlined) {
		this.underlined = underlined;
		redraw();
	}
	/**
	 * Returns the underline state of the hyperlink.
	 * 
	 * @return <samp>true </samp> if text is underlined, <samp>false </samp>
	 *         otherwise.
	 */
	public boolean isUnderlined() {
		return underlined;
	}
	/**
	 * Overrides the parent by incorporating the margin.
	 */
	public Point computeSize(int wHint, int hHint, boolean changed) {
		checkWidget();
		int innerWidth = wHint;
		if (innerWidth != SWT.DEFAULT)
			innerWidth -= marginWidth * 2;
		Point textSize = computeTextSize(innerWidth, hHint);
		int textWidth = textSize.x + 2 * marginWidth;
		int textHeight = textSize.y + 2 * marginHeight;
		return new Point(textWidth, textHeight);
	}

	/**
	 * Returns the current hyperlink text.
	 * 
	 * @return hyperlink text
	 */
	public String getText() {
		return text;
	}
	/**
	 * Sets the text of this hyperlink.
	 * 
	 * @param text
	 *            the hyperlink text
	 */
	public void setText(String text) {
		if (text != null)
			this.text = text;
		else
			text = "";
		redraw();
	}
	/**
	 * Paints the hyperlink text.
	 * 
	 * @param e
	 *            the paint event
	 */
	protected void paintHyperlink(PaintEvent e) {
		GC gc = e.gc;
		int x = marginWidth;
		int y = marginHeight;
		Point size = getSize();
		Rectangle bounds = new Rectangle(x, y, size.x - marginWidth
				- marginWidth, size.y - marginHeight - marginHeight);
		paintText(gc, bounds);
	}
	/**
	 * Paints the hyperlink text in provided bounding rectangle.
	 * @param gc graphic context
	 * @param bounds the bounding rectangle in which to paint the text
	 */
	protected void paintText(GC gc, Rectangle bounds) {
		gc.setFont(getFont());
		gc.setForeground(getForeground());
		if ((getStyle() & SWT.WRAP) != 0) {
			FormUtil.paintWrapText(gc, text, bounds, underlined);
		} else {
			gc.drawText(getText(), marginWidth, marginHeight, true);
			if (underlined) {
				FontMetrics fm = gc.getFontMetrics();
				int descent = fm.getDescent();
				int lineY = bounds.y + bounds.height - descent + 1;
				gc.drawLine(marginWidth, lineY, bounds.width, lineY);
			}
		}
	}
	private Point computeTextSize(int wHint, int hHint) {
		Point extent;
		GC gc = new GC(this);
		gc.setFont(getFont());
		if ((getStyle() & SWT.WRAP) != 0 && wHint != SWT.DEFAULT) {
			extent = FormUtil.computeWrapSize(gc, text, wHint);
		} else {
			extent = gc.textExtent(getText());
		}
		gc.dispose();
		return extent;
	}
}
