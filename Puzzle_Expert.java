import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.measure.*;
import java.util.Random;
import java.awt.event.*;
import ij.text.TextWindow; 
import ij.text.TextPanel;
import java.io.*;
import java.util.HashMap;

  /**
   * Plugin converts an image to black and white and draws a rectangle around each shape, then the user assigns letters to rectangles manually in a window
   * and the results are saved as 12x12 images when the user closes the window.      
   */     

public class Puzzle_Expert extends ParticleAnalyzer implements PlugInFilter, MouseListener, KeyListener, ImageListener {

  protected int w, h, u, v, p, i, yTopBorder, yBottomBorder, xLeftBorder, xRightBorder,
    r, g, b, rColor;
  
  protected float[] x, y, widths, heights;
  
  protected ImageProcessor ipOrig, ipOrigWithRectangles, ipNew, ipNewWithRectangles, ipNewBw;

  ImagePlus imRes;

  ImageWindow win;
  ImageCanvas canvas;
  LetterTextWindow tw;
  
  CurrentSelectedPosition currentSelectedPosition = new CurrentSelectedPosition();
  
  char keyChar;
  int keyCode;
  
  int sliceNumber = 1;
  
  String imageTitle;
  
  /**
   * Constructor.
   *    
   * Initialize the base class with pre-set options and measurements.       
   */ 

  public Puzzle_Expert() {

    this(128, 513, Analyzer.getResultsTable(), 0.0, Double.POSITIVE_INFINITY, 0.0, 1.0);
           
  }

  /**
   * Constructor with input parameters.
   * 
   * See base class for explanation.     
   */

  public Puzzle_Expert(int options, int measurements, ResultsTable rt, double minSize, double maxSize, double minCirc, double maxCirc) {
    super(options, measurements, rt, minSize, maxSize, minCirc, maxCirc);
  }


  /**
   * Setup the plugin.
   *    
   * Convert the image to grayscale right away or the base class will reject it.
   *    
   * @param String arg Flag used by the base class
   * @param ImagePlus img The plugin's image object
   *    
   * @return int flags Plugin configuration flags         
   */ 
  //@Override 
  public int setup(String arg, ImagePlus imp) {
    
	  imageTitle = imp.getShortTitle();
	  
    //if no image, exit
    if (imp==null)
			{IJ.noImage();return DONE;}

    //reset results table to erase past data
    rt.reset();

    //we have to convert the image to grayscale right away or the base class will reject it
    ImageConverter ic = new ImageConverter(imp);
    ic.convertToGray8();
  
    int flags = super.setup(arg, imp);
    return flags;
  }

  /**
   * Run the plugin.
   *    
   * Steps:
   * (i) Convert the image to black-or-white only based on an arbitrary threshold
   * (ii) Run the base class method to identify clusters in the image
   * (iii) Display the rectangles around each object that were calculated by the base class method         
   *    
   * @param ImageProcessor im The plugin's image processor object, used to manipulate the image       
   */
   @Override 
	public void run(ImageProcessor ip) {
    
    ip = ip.rotateRight();
    
    //copy the image processor to our global object so we don't destroy the original
    ipNew = ip.duplicate();
    
    //set threshold & convert the image to black-or-white only
    ipNew.setAutoThreshold("IsoData", false, ImageProcessor.BLACK_AND_WHITE_LUT);
    /*//manual method of setting threshold
    int[] his = ipNew.getHistogram();
    int g = findThreshold(his);
    ipNew.setThreshold(0.0, (double) g, ImageProcessor.BLACK_AND_WHITE_LUT);*/
                      
    //update the global image object
    imp = new ImagePlus("result", ipNew);

    //calculate width and height of image
	  w = ipNew.getWidth();
	  h = ipNew.getHeight(); 

    //run the base class method to calculate the positions of the rectangles around each cluster
    getClusters();     
 
    //create duplicates for display
    //the original image
    ipOrig = ip.duplicate().convertToRGB();
    //a copy of the original image that we will draw rectangles on
    ipOrigWithRectangles = ipOrig.duplicate();
    //a black-and-white only (binary) copy of the image, which we will draw rectangles on 
    ipNewWithRectangles = ipNew.convertToRGB();
    //another black-and-white only (binary) copy, without rectangles, for creating the individual letter images
    ipNewBw = ipNewWithRectangles.duplicate();
    
    //get x "start values" of rectangles (upper left-hand corner)
    x = rt.getColumn(11);
    //get y "start values" of rectangles (upper left-hand corner)
    y = rt.getColumn(12);
    //get widths of rectangles
    widths = rt.getColumn(13);
    //get heights of rectangles
    heights = rt.getColumn(14);
    
    //add a rectangle around each cluster
    addRectangles();
    
    //display the results
    displayResults();
	}

  /**
   * Find clusters of pixels in the image and store the positions of the rectangles around each one
   * in the rt (ResultsTable) object.         
   */
  public void getClusters()
  {
		slice++;
		if (imp.getStackSize()>1 && processStack)
			imp.setSlice(slice);
	  
		analyze(imp, ipNew);
  }
  
  /**
   * Override this method so that the dialog with display options is not shown.
   * 
   * @return bool True if successful   
   */
  
  //@Override 
	public boolean showDialog() {
 				
		return true;
	}

  /**
   * Add the rectangles around each cluster to the image.
   */
  protected void addRectangles()
  {
     //set rectangle color
     r = 0; g = 0; b = 255;
     //convert the r, g, b components back to an integer
     rColor = ((r & 0xff) << 16) +
            ((g & 0xff) << 8) +
            (b & 0xff);
          //update the pixel in the color image

     //draw the rectangles
     for (i=0; i<x.length; i++)
     {
        if (heights[i] > 5 && widths[i] > 5) //arbitrary minimum dimensions for the rectangle
        {
          //calculate the x and y positions of the borders of the rectangle
          yTopBorder = (int) y[i];
          yBottomBorder = yTopBorder + (int) heights[i];
          xLeftBorder = (int) x[i];
          xRightBorder = xLeftBorder + (int) widths[i];
        
          if (yBottomBorder >= h || xRightBorder > w)
          {
            continue;
          }
        
          //draw the top and bottom borders of the rectangle
          for(u = (int) x[i]; u < (int) (x[i] + widths[i]); u++)
          {
            //draw top border
            ipNewWithRectangles.set(u, yTopBorder, rColor);
            ipOrigWithRectangles.set(u, yTopBorder, rColor);
            
            //draw bottom border            
            ipNewWithRectangles.set(u, yBottomBorder, rColor);
            ipOrigWithRectangles.set(u, yBottomBorder, rColor);  
          }
          
          //draw the left and right borders of the rectangle
          for(v = (int) y[i]; v < (int) (y[i] + heights[i]); v++)
          {
            //draw top border
            ipNewWithRectangles.set(xLeftBorder, v, rColor);
            ipOrigWithRectangles.set(xLeftBorder, v, rColor);
            
            //draw bottom border
            ipNewWithRectangles.set(xRightBorder, v, rColor);
            ipOrigWithRectangles.set(xRightBorder, v, rColor);  
          }
        }
     }
  }

  /**
   * Display the images.
   */
  protected void displayResults()
  {
    //create a copy of the original image
    ImagePlus imOrig = new ImagePlus("original", ipOrig);

    //create an image stack
    ImageStack stack = imOrig.getStack();
    
    //add the image with the rectangles to the stack
    stack.addSlice("with rectangles", ipOrigWithRectangles);
    
    //add the black and white version with the rectangles to the stack
    stack.addSlice("black & white", ipNewWithRectangles);

    //display the stack: the user presses keyboard left or right to toggle the images
    imRes = new ImagePlus("results", stack);
    imRes.show();
    
    //add mouse onclick events
    win = imRes.getWindow();
    //due to image rotation, change the dimensions of the window
    Rectangle bounds = win.getBounds();
    win.setLocationAndSize(bounds.x, bounds.y, 2*bounds.height, 2*bounds.width);
    
    canvas = win.getCanvas();

    win.addMouseListener(this);
    canvas.addMouseListener(this);   
    //add keyboard events
    win.removeKeyListener(IJ.getInstance());
    canvas.removeKeyListener(IJ.getInstance());

    win.addKeyListener(this);
    canvas.addKeyListener(this);
    ImagePlus.addImageListener(this);
    //add window to save data from onclick and keyboard events
    String title = "Key mapping table";
    String headings = "x\ty\tletter\t";
    tw = new LetterTextWindow(title, headings, "", 400, 500, this);
  }
  
  /**
   * Saves the position of the last place the user clicked on with the mouse.         
   */
  
  protected class CurrentSelectedPosition
  {
    public int x = 0;
    public int y = 0;
  };

  /**
   * Onclick event: when user clicks, save the coordinate position, and the next time they press
   * a letter key, print the coordinates and that letter to the window.
   * 
   * @param MouseEvent The onclick event                  
   */

  public void mouseClicked(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    int offscreenX = canvas.offScreenX(x);
    int offscreenY = canvas.offScreenY(y);
    
    currentSelectedPosition.x = offscreenX;
    currentSelectedPosition.y = offscreenY;
  }

  /**
   * Event that is fired when the user presses a key.
   * 
   * If the key is a letter, and the user has previously clicked on a place in the image,
   * assign the letter to those coordinates.         
   * 
   * @param KeyEvent The keypress event                  
   */

  public void keyPressed(KeyEvent e) {

    keyCode = e.getKeyCode();
    keyChar = e.getKeyChar();
    //if it's not a letter, do nothing
    if (Character.isLetter(keyChar))
    {
        //if the user has not clicked the mouse to select a position, don't do anything
        if (currentSelectedPosition.x == 0)
        {
          return;
        }
        tw.append(currentSelectedPosition.x+"\t"+currentSelectedPosition.y+"\t"+KeyEvent.getKeyText(keyCode));
        currentSelectedPosition.x = currentSelectedPosition.y = 0;
    } else
    {
        //little hack: we have to restore the functionality of the direction keys because they were canceled when we
        //removed all window events in order to cancel the keyboard macros
        if (keyCode == KeyEvent.VK_UP)
        {
            //up key: zoom in
            canvas.zoomIn(canvas.getWidth()/2, canvas.getHeight()/2);
        } else if (keyCode == KeyEvent.VK_DOWN)
        {
            //down key: zoom out
            canvas.zoomOut(canvas.getWidth()/2, canvas.getHeight()/2);
        } else if (keyCode == KeyEvent.VK_RIGHT)
        {
            //right key: show next image in stack
            if (sliceNumber > 2)
            {
              sliceNumber = 1;
            } else
            {
              sliceNumber++; 
            }
            imRes.setSlice(sliceNumber);
        } else if (keyCode == KeyEvent.VK_LEFT)
        {
            //left key: show previous image in stack
            if (sliceNumber < 2)
            {
              sliceNumber = 3;
            } else
            {
              sliceNumber--; 
            }
            imRes.setSlice(sliceNumber);
        }
     }
  }

  /**
   * Event that is fired when the image is closed.
   * 
   * Remove listeners.               
   * 
   * @param ImagePlus The image object      
   */

  public void imageClosed(ImagePlus imp) {
    if (win!=null)
      win.removeKeyListener(this);
    if (canvas!=null)
      canvas.removeKeyListener(this);
    ImagePlus.removeImageListener(this);
  }

  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) {}
  public void imageOpened(ImagePlus imp) {}
  public void imageUpdated(ImagePlus imp) {}

  /**
   * Manual threshold calculation.
   */

  /*private int findThreshold(int[] data)
  {
		int i, l, totl, g=0;
		double toth, h;
		for (i = 1; i < 256; i++) {
			if (data[i] > 0){
				g = i + 1;
				break;
			}
		}
		while (true){
			l = 0;
			totl = 0;
			for (i = 0; i < g; i++) {
				 totl = totl + data[i];
				 l = l + (data[i] * i);
			}
			h = 0;
			toth = 0;
			for (i = g + 1; i < 256; i++){
				toth += data[i];
				h += ((double)data[i]*i);
			}
			if (totl > 0 && toth > 0){
				l /= totl;
				h /= toth;
				if (g == (int) Math.round((l + h) / 2.0))
					break;
			}
			g++;
			if (g > 254)
				return -1;
		}
		return g;  
  }*/

  /**
   * Class holds the coordinates of a rectangle around a cluster, allowing us to manipulate them easily.
   *             
   */ 

  protected class RectangleCoordinates
  {
  /**
   * Coordinates of a rectangle: x-position of top-left corner, y-position of top-left corner,
   * width of rectangle, height of rectangle         
   */ 
	  public int xTopLeft, yTopLeft, rW, rH;

  /**
   * Adjust the rectangle dimensions to a square.
   *    
   * This helps because later we want to resize the image to 12x12.         
   */ 

    public void adjustToSquare()
    {
    	//IJ.log("original coordinates: xTopLeft="+xTopLeft+", yTopLeft="+yTopLeft+", rW="+rW+", rH="+rH);
    	
    	//we need a square shape for the resizing, so adjust the parameters slightly
		if (rH > rW)
		{
			if ((rH - rW) % 2 != 0)
			{
				rH = rH + 1;
			}
			//make sure the horizontal position of the center does not change, unless it violates the boundaries of the image
			if ((xTopLeft - (rH - rW)/2) > -1)
			{
				xTopLeft = xTopLeft - (rH - rW)/2;
			} else
			{
				xTopLeft = 0;
			}
			rW = rH;
		} else if (rW > rH)
		{
			if ((rW - rH) % 2 != 0)
			{
				rH = rH + 1;
			}
			//make sure the vertical position of the center does not change, unless it violates the boundaries of the image
			if ((yTopLeft - (rW - rH)/2) > -1)
			{
				yTopLeft = yTopLeft - (rW - rH)/2;
			} else
			{
				yTopLeft = 0;
			}
			rH = rW;
		}
		
		//IJ.log("final coordinates: xTopLeft="+xTopLeft+", yTopLeft="+yTopLeft+", rW="+rW+", rH="+rH);
    }
  };
  
  /**
   * Saves all of the letters in the image as individual 12x12 images.
   * 
   * This function is called when the "LetterTextWindow" (the results table holding the coordinates
   * of the letters) is closed by the user.
   *      
   * @param Array xUserClick Array of x-positions where the user has clicked
   * @param Array yUserClick Array of y-positions where the user has clicked
   * @param Array letters Array of letters that user has assigned manually to each spot where they have clicked           
   *        
   */
  
  public void saveLettersAsImages(Integer[] xUserClick, Integer[] yUserClick, String[] letters, String text)
  {
    //holds number of occurrences of each letter
	  Integer letterCount;
    //holds the letter
	  String letter;
    //ImageProcessor object holding the individual letter image
	  ImageProcessor letterIp, letterIpBw;
    //ImagePlus object holding the individual letter image
    ImagePlus letterIm, letterImBw;
    //holds the coordinates of each rectangle, which we may want to manipulate before saving
	  RectangleCoordinates rectangleCoordinates = new RectangleCoordinates();
	  //keep track of how many of each letter we need to store as an image, for image naming purposes
	  HashMap<String, Integer> letterCountMap = new HashMap<String, Integer>();
	  
	  //create file structure to hold images: the 12x12 letter images for each big image are stored in /tmp/[big image name]
	  File dir = new File("tmp");
	  if (!dir.exists())
	  {
		  dir.mkdir();
	  }
	  dir = new File("tmp/" + imageTitle);
	  if (!dir.exists())
	  {
		  dir.mkdir();
	  }
    //create extra directory for the black and white versions
	  dir = new File("tmp/" + imageTitle + "/bw");
	  if (!dir.exists())
	  {
		  dir.mkdir();
	  }

	  //cycle through all the rectangles we identified from the image, see if the user has clicked on any of them
	  for(int i = 0; i < x.length; i++)
	  {
		  //exclude rectangle if it is so big that it is obviously not a letter (e.g. a big rectangle around part of the image)
		  if (((float) heights[i] >= 0.2f * (float) h) || ((float) widths[i] >= 0.2f * (float) w))
		  {
			  continue;
		  }
		  
		  for(int j = 0; j < xUserClick.length; j++)
		  {			  
			  //check if the user click falls within the bounds of the rectangle
			  if (
					  (xUserClick[j] > x[i])
					  && (xUserClick[j] < (x[i] + widths[i]))
					  && (yUserClick[j] > y[i])
					  && (yUserClick[j] < (y[i] + heights[i]))
			  )
			  {
				  //user has clicked on this rectangle and identified the shape it contains as a particular letter
          letter = letters[j];
				  rectangleCoordinates.xTopLeft = (int) x[i];
				  rectangleCoordinates.yTopLeft = (int) y[i];
				  rectangleCoordinates.rW = (int) widths[i];
				  rectangleCoordinates.rH = (int) heights[i];
				  
				  //we need a square shape for the resizing, so adjust the parameters of the rectangle slightly
				  rectangleCoordinates.adjustToSquare();
				  
          //log our progress in a window
				  IJ.log(letter + ": top left position=(" + rectangleCoordinates.xTopLeft + ", " + rectangleCoordinates.yTopLeft +
				    	    "), height=" + rectangleCoordinates.rH + ", width=" + rectangleCoordinates.rW);
				  //standard version
          letterIp = new ByteProcessor(rectangleCoordinates.rW, rectangleCoordinates.rH);
          //black and white version
          letterIpBw = new ByteProcessor(rectangleCoordinates.rW, rectangleCoordinates.rH);
          
				  //copy the rectangle from the original image to a new image
				  for(u = 0; u < rectangleCoordinates.rW; u++)
					{
						for(v = 0; v < rectangleCoordinates.rH; v++)
						{
							p = ipOrig.get((rectangleCoordinates.xTopLeft + u), (rectangleCoordinates.yTopLeft + v));
							letterIp.set(u, v, p);
              p = ipNewBw.get((rectangleCoordinates.xTopLeft + u), (rectangleCoordinates.yTopLeft + v));
							letterIpBw.set(u, v, p);
						}
					}

				  //resize the image to an 12x12 image using linear interpolation
				  letterIp.setInterpolationMethod(ImageProcessor.BILINEAR);
				  letterIp = letterIp.resize(12);
          
          letterIpBw.setInterpolationMethod(ImageProcessor.BILINEAR);
				  letterIpBw = letterIpBw.resize(12);
				    
				  //keep track of how many of each letter we have
				  letterCount = letterCountMap.get(letter);
				  if (letterCount != null)
				  {
				   	letterCount++;
				  } else
				  {
				   	letterCount = 1;
				  }
				  letterCountMap.put(letter, letterCount);

				  letterIm = new ImagePlus(letter, letterIp);
          letterImBw = new ImagePlus(letter, letterIpBw);
				  //letterImBw.show();
				  
          //save the 12x12 letter image in the /tmp/[big image name]/ folder  
				  IJ.saveAs(letterIm, "png", "tmp/" + imageTitle + "/" + letter + letterCount + ".png");
          //save the black and white version in the /tmp/[big image name]/bw/ folder
          IJ.saveAs(letterImBw, "png", "tmp/" + imageTitle + "/bw/" + letter + letterCount + ".png");
				  
          IJ.log("image saved as tmp/" + imageTitle + "/" + letter + letterCount + ".png");
            
				  break;
			  }
		  }
	  }

    //finally, save the table mapping the image coordinates to letters
    try{
      // Create file 
      FileWriter fstream = new FileWriter("tmp/" + imageTitle + "/results.txt");
      BufferedWriter out = new BufferedWriter(fstream);
      out.write(text);
      //Close the output stream
      out.close();
    }catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
    }


    //out.write("xxx");



  }

  
}
