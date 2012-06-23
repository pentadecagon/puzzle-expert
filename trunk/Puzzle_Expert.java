import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.measure.*;
import java.util.Random;

  /**
   * Plugin converts an image to black and white and draws a rectangle around each shape.
   */     

public class Puzzle_Expert extends ParticleAnalyzer {

  protected int w, h, u, v, p, i, yTopBorder, yBottomBorder, xLeftBorder, xRightBorder,
    r, g, b, rColor;
  
  protected float[] x, y, widths, heights;
  
  protected ImageProcessor ipOrig, ipOrigMod, ipNew, ipNewMod;

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

    //if no image, exit
    if (imp==null)
			{IJ.noImage();return DONE;}

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
  
    //copy the image processor to our global object so we don't destroy the original
    ipNew = ip.duplicate();

    //update the global image object
    imp = new ImagePlus("result", ipNew);
    
    //calculate width and height of image
		w = ipNew.getWidth();
		h = ipNew.getHeight(); 
         
    //convert the image to black-or-white only
    convertImageToBinaryMap();

    //run the base class method to calculate the positions of the rectangles around each cluster
    getClusters();     

    //create duplicates for display
    ipOrig = ip.duplicate().convertToRGB();
    ipOrigMod = ipOrig.duplicate();
    ipNewMod = ipNew.duplicate().convertToRGB();
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
   * Convert the image from greyscale into black-or-white only based on an arbitrary threshold. 
   */
  protected void convertImageToBinaryMap()
  {
    //loop over all image coordinates and convert the pixels to either black or
    //white based on an arbitrary threshold
		for(u = 0; u < w; u++)
		{
			for(v = 0; v < h; v++)
			{
        //get the pixel at this position in the grayscale image
				p = ipNew.get(u, v);
        if (p < 100) //arbitrary cutoff for "black"
        {
          ipNew.set(u, v, 0); 
        } else
        {
          ipNew.set(u, v, 255);
        }
			}
		}
  }
 

  /**
   * Add the rectangles around each cluster to the image.
   */
  protected void addRectangles()
  {
     //get x "start values" of rectangles (upper left-hand corner)
     x = rt.getColumn(11);
     //get y "start values" of rectangles (upper left-hand corner)
     y = rt.getColumn(12);
     //get widths of rectangles
     widths = rt.getColumn(13);
     //get heights of rectangles
     heights = rt.getColumn(14);
     
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
            ipNewMod.set(u, yTopBorder, rColor);
            ipOrigMod.set(u, yTopBorder, rColor);
            
            //draw bottom border            
            ipNewMod.set(u, yBottomBorder, rColor);
            ipOrigMod.set(u, yBottomBorder, rColor);  
          }
          
          //draw the left and right borders of the rectangle
          for(v = (int) y[i]; v < (int) (y[i] + heights[i]); v++)
          {
            //draw top border
            ipNewMod.set(xLeftBorder, v, rColor);
            ipOrigMod.set(xLeftBorder, v, rColor);
            
            //draw bottom border
            ipNewMod.set(xRightBorder, v, rColor);
            ipOrigMod.set(xRightBorder, v, rColor);  
          }
        }
     }
  }

  /**
   * Display the images.
   */
  protected void displayResults()
  {
    ImagePlus imOrig = new ImagePlus("original", ipOrig);

    ImageStack imStack = imOrig.getStack();
    imStack.addSlice("with rectangles", ipOrigMod);
    imStack.addSlice("black & white", ipNewMod);

    ImagePlus imTest = new ImagePlus("results", imStack);
    imTest.show();
  }

}
