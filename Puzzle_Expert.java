import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.Random; 

  /**
   * Plugin converts the black parts of an image to red
   */     

public class Puzzle_Expert implements PlugInFilter {

	public int setup(String arg, ImagePlus im) {
    return DOES_RGB + NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
  
    //calculate width and height of image
		int w = ip.getWidth();
		int h = ip.getHeight();

    //copy the image to an 8-bit grayscale (don't rescale)
    ImageProcessor ipGray = ip.convertToByte(false);

    //initialize variables
    int p, pNew, r, g = 0, b = 0;
    
    //loop over all image coordinates and convert the black pixels to red
		for(int u = 0; u < w; u++)
		{
			for(int v = 0; v < h; v++)
			{
        //get the pixel at this position in the grayscale image
				p = ipGray.get(u, v);
        if (p < 100) //arbitrary cutoff for "black"
        {
          //convert the pixel to red in the color image
          r = 255;
          //convert the r, g, b components back to an integer
          pNew = ((r & 0xff) << 16) +
            ((g & 0xff) << 8) +
            (b & 0xff);
          //update the pixel in the color image
          ip.set(u, v, pNew); 
        }
			}
		}
    
    //display the modified image
    String imFinalTitle = "new image";
    ImagePlus imFinal = new ImagePlus(imFinalTitle, ip);
    imFinal.show();

	}

}
