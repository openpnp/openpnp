/**
 * AlphaImageIcon class written by Darryl from
 * https://tips4java.wordpress.com/2010/08/22/alpha-icons/
 *
 * Open licence code.
 */
package org.openpnp.gui;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.ImageObserver;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * An Icon  wrapper that paints the contained icon with a specified transparency.
 * <P>
 * This class is suitable for wrapping an <CODE>ImageIcon</CODE>
 * that holds an animated image.  To show a non-animated Icon with transparency,
 * the companion class {@link AlphaIcon} is a lighter alternative.
 *
 * @version 1.0 08/16/10
 * @author Darryl
 */
public class AlphaImageIcon extends ImageIcon {

  private Icon icon;
  private Image image;
  private float alpha;

  /**
   * Creates an <CODE>AlphaImageIcon</CODE> with the specified icon and opacity.
   * The opacity <CODE>alpha</CODE> should be in the range 0.0F (fully transparent)
   * to 1.0F (fully opaque).
   *
   * @param icon the Icon to wrap
   * @param alpha the opacity
   */
  public AlphaImageIcon(Icon icon, float alpha) {
    this.icon = icon;
    this.alpha = alpha;
  }

  /**
   * Overridden to return the image of a wrapped ImageIcon, or null if the wrapped icon
   * is not an ImageIcon.
   *
   * @return the Image object for a wrapped ImageIcon, or null
   */
  @Override
  public Image getImage() {
    return image;
  }

  /**
   * Overridden to forward to a wrapped ImageIcon.  Does nothing if the wrapped icon
   * is not an ImageIcon.
   * <P>
   * In common with <code>ImageIcom</code>, the newly set image will only be shown when the
   * concerned component(s) are repainted.
   *
   * @param image Sets the image displayed by a wrapped ImageIcon
   */
  @Override
  public void setImage(Image image) {
    if (icon instanceof ImageIcon) {
      ((ImageIcon) icon).setImage(image);
    }
  }

  /**
   * Overridden to return the status of the image loading operation of a wrapped ImageIcon,
   * or 0 if the contained icon is not an ImageIcon.
   *
   * @return the loading status as defined by java.awt.MediaTracker
   */
  @Override
  public int getImageLoadStatus() {
    if (icon instanceof ImageIcon) {
      return ((ImageIcon) icon).getImageLoadStatus();
    }
    return 0;
  }

  /**
   * Overridden to return the ImageObserver of the image of a wrapped ImageIcon, or null if
   * the contained icon is not an ImageIcon.
   *
   * @return the loading status as defined by java.awt.MediaTracker
   */
  @Override
  public ImageObserver getImageObserver() {
    if (icon instanceof ImageIcon) {
      return ((ImageIcon) icon).getImageObserver();
    }
    return null;
  }

  /**
   * Overridden to forward to a wrapped ImageIcon.  Does nothing if the wrapped icon is
   * not an ImageIcon.
   *
   * @param observer the image observer
   */
  @Override
  public void setImageObserver(ImageObserver observer) {
    if (icon instanceof ImageIcon) {
      ((ImageIcon) icon).setImageObserver(observer);
    }
  }

  /**
   * Gets this <CODE>AlphaIcon</CODE>'s opacity
   * @return the opacity, in the range 0.0 to 1.0
   */
  public float getAlpha() {
    return alpha;
  }

  /**
   * Gets the icon wrapped by this <CODE>AlphaIcon</CODE>
   * @return the wrapped icon
   */
  public Icon getIcon() {
    return icon;
  }

  /**
   * Paints the wrapped icon with this <CODE>AlphaImageIcon</CODE>'s transparency.
   *
   * @param c The component to which the icon is painted
   * @param g the graphics context
   * @param x the X coordinate of the icon's top-left corner
   * @param y the Y coordinate of the icon's top-left corner
   */
  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    if (icon instanceof ImageIcon) {
      image = ((ImageIcon) icon).getImage();
    } else {
      image = null;
    }
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setComposite(AlphaComposite.SrcAtop.derive(alpha));
    icon.paintIcon(c, g2, x, y);
    g2.dispose();
  }

  /**
   * Gets the width of the bounding rectangle of this <CODE>AlphaImageIcon</CODE>.
   * Overridden to return the width of the wrapped icom.
   *
   * @return the width in pixels
   */
  @Override
  public int getIconWidth() {
    return icon.getIconWidth();
  }

  /**
   * Gets the height of the bounding rectangle of this <CODE>AlphaImageIcon</CODE>.
   * Overridden to return the height of the wrapped icon.
   *
   * @return the height in pixels
   */
  @Override
  public int getIconHeight() {
    return icon.getIconHeight();
  }
}
