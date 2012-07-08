import ij.*;
import ij.text.*;
import ij.gui.*;

  /**
   * This is a results window that opens when the plugin is initialized. The user has to click on points in the image and assign letters to those points.
   * Then when the user closes this window, the rectangles of interest they clicked on will be saved as 12x12 images.   
   */ 

public class LetterTextWindow extends TextWindow {

  //the "owner" class
	Puzzle_Expert puzzleExpert;
	
  //holds the x- and y- coordinates of the rectangles of interest the user has clicked on
	Integer[] x, y;
  //holds the letters that the user has assigned to rectangles of interest
	String[] letters;
  //temporary arrays for parsing the user's results into the arrays above
	String[] lines, elements;
  
  /**
   * Constructor
   */ 
  
	public LetterTextWindow(String title, String headings, String data, int width, int height, Puzzle_Expert puzzleExpert) {		
		super(title, headings, data, width, height);
		this.puzzleExpert = puzzleExpert;
	}

  /**
   * When the user closes the window, shows a dialog saying "images will be saved", and if the user clicks ok, parses the
   * results into arrays and passes them back to the main class for processing into images.
   * 
   * @param boolean showDialog Whether a dialog should be shown or not, default true            
   */ 

  @Override
	public void close(boolean showDialog) {
			    
		TextPanel tp = this.getTextPanel();
		String text = tp.getText();
		
		//if we could not get any results from the text, close
		if (!getResultsFromText(text))
		{
			//close
			dispose();
			WindowManager.removeWindow(this);
			return;
		}
		
		//show a dialog asking the user whether to confirm whether to save the letters as individual images
	    GenericDialog gd = new GenericDialog("Save letters");
	    gd.addMessage("Save letters as images?");
	    gd.showDialog();
	    
	    if (gd.wasCanceled())
	    {
			return;
	    }

	    puzzleExpert.saveLettersAsImages(x, y, letters, text);	    

		//close
		dispose();
		WindowManager.removeWindow(this);
		return;
	}
	
  /**
   * Parses the results in the results window into arrays for processing.
   * 
   * @param string text The full text contained in the results window
   * @return boolean True if information was successfully obtained from the results window, otherwise false               
   */
  
	protected boolean getResultsFromText(String text)
	{
      //split the text into its individual lines
      lines = text.split("\n");
        
      //if the only line is the header, we can't retrive any results
      if (lines.length < 2)
      {
        return false;
      }
        
      //ignore the first line (the headers)
      x = new Integer[lines.length - 1];
      y = new Integer[lines.length - 1];
      letters = new String[lines.length - 1];

      for(int i = 0; i < lines.length; i++)
      {
        if (i > 0)
        {
       	 	elements = lines[i].split("\t");
          //the first element in the line is the x-position (top left) of the rectangle of interest
       	 	x[i - 1] = Integer.parseInt(elements[0]);
          //the second element in the line is the y-position (top left) of the rectangle of interest
       	 	y[i - 1] = Integer.parseInt(elements[1]);
          //the third element in the line is letter the user has assigned to the rectangle of interest
       	 	letters[i - 1] = elements[2];
        }
      }
        
      return true;
	}
	
} 