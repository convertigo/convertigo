package com.twinsoft.convertigo.eclipse;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

public class AnimatedGif {

	private Thread animationThread;
	private boolean bContinue = true;
	private Display display;
	private boolean useGIFBackground = false;
	private Canvas animationCanvas;
	private ImageLoader loader;
	private ImageData[] imageDataArray;
	private GC animationCanvasGC;
	private Color shellBackground;

	public AnimatedGif(Display display, Canvas animationCanvas, String animatedGifFile) {
		this.display = display;
		this.animationCanvas = animationCanvas;

		try {
			loader = new ImageLoader();
		} catch (SWTException ex) {
			ConvertigoPlugin.logException(ex, "There was an error loading the GIF", false);
			loader = null;
		}
		imageDataArray = loader.load(getClass().getResourceAsStream(animatedGifFile));
		animationCanvasGC = new GC(animationCanvas);
		shellBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
	}
	
	public void start() {
		if (loader == null) return;
		
		if (imageDataArray.length > 1) {
			animationThread = new Thread(new Runnable() {
				public void run() {
					/*
					 * Create an off-screen image to draw on, and fill it
					 * with the shell background.
					 */
					Image offScreenImage = new Image(display, loader.logicalScreenWidth,
							loader.logicalScreenHeight);
					GC offScreenImageGC = new GC(offScreenImage);
					offScreenImageGC.setBackground(shellBackground);
					offScreenImageGC.fillRectangle(0, 0, loader.logicalScreenWidth,
							loader.logicalScreenHeight);

					Image image = null;

					try {
						/*
						 * Create the first image and draw it on the
						 * off-screen image.
						 */
						int imageDataIndex = 0;
						ImageData imageData = imageDataArray[imageDataIndex];
						image = new Image(display, imageData);
						offScreenImageGC.drawImage(image, 0, 0, imageData.width, imageData.height,
								imageData.x, imageData.y, imageData.width, imageData.height);

						/*
						 * Now loop through the images, creating and drawing
						 * each one on the off-screen image before drawing
						 * it on the shell.
						 */
						int repeatCount = loader.repeatCount;
						while ((loader.repeatCount == 0 || repeatCount > 0) && bContinue) {
							switch (imageData.disposalMethod) {
							case SWT.DM_FILL_BACKGROUND:
								/*
								 * Fill with the background color before
								 * drawing.
								 */
								Color bgColor = null;
								if (useGIFBackground && loader.backgroundPixel != -1) {
									bgColor = new Color(display,
											imageData.palette.getRGB(loader.backgroundPixel));
								}
								offScreenImageGC
										.setBackground(bgColor != null ? bgColor : shellBackground);
								offScreenImageGC.fillRectangle(imageData.x, imageData.y, imageData.width,
										imageData.height);
								if (bgColor != null)
									bgColor.dispose();
								break;
							case SWT.DM_FILL_PREVIOUS:
								/*
								 * Restore the previous image before
								 * drawing.
								 */
								offScreenImageGC.drawImage(image, 0, 0, imageData.width, imageData.height,
										imageData.x, imageData.y, imageData.width, imageData.height);
								break;
							}

							imageDataIndex = (imageDataIndex + 1) % imageDataArray.length;
							imageData = imageDataArray[imageDataIndex];
							image.dispose();
							image = new Image(display, imageData);
							offScreenImageGC.drawImage(image, 0, 0, imageData.width, imageData.height,
									imageData.x, imageData.y, imageData.width, imageData.height);

							/* Draw the off-screen image to the shell. */
							animationCanvasGC.drawImage(offScreenImage, 0, 0);

							/*
							 * Sleep for the specified delay time (adding
							 * commonly-used slow-down fudge factors).
							 */
							try {
								int ms = imageData.delayTime * 10;
								if (ms < 20)
									ms += 30;
								if (ms < 30)
									ms += 10;
								Thread.sleep(ms);
							} catch (InterruptedException e) {
							}

							/*
							 * If we have just drawn the last image,
							 * decrement the repeat count and start again.
							 */
							if (imageDataIndex == imageDataArray.length - 1)
								repeatCount--;
						}
					} catch (Exception ex) {
						ConvertigoPlugin.logException(ex, "There was an error animating the GIF", false);
					} finally {
						if (offScreenImage != null && !offScreenImage.isDisposed())
							offScreenImage.dispose();
						if (offScreenImageGC != null && !offScreenImageGC.isDisposed())
							offScreenImageGC.dispose();
						if (image != null && !image.isDisposed())
							image.dispose();
					}
					ConvertigoPlugin.logDebug("End of GIF animation thread");
				}
			});

			animationThread.setDaemon(true);
			animationThread.setName("AnimatedGif");

			bContinue = true;
			animationCanvas.setVisible(true);
			
			animationThread.start();
		}
	}

	public void stop() {
		bContinue = false;
		animationCanvas.setVisible(false);
	}
}
