package ch.cern.cms.data_browser.GLFont;

import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Point;

import com.threed.jpct.FrameBuffer;
import com.threed.jpct.RGBColor;

/**
 * <p>creates GL renderable (blittable) font out of given AWT font. 
 * a jPCT texture is created and added to TextureManager on the fly.</p>
 *  
 * <p>in contrast with its name, this class can be used for software renderer too.
 * but to tell the truth, i would stick to Java2D for software renderer ;)</p>    
 * 
 * this class uses {@link TexturePack} behind the scenes.
 * 
 * @see TexturePack 
 * 
 * @author hakan eryargi (r a f t)
 */
public class AGLFont {
	/** standard characters */
	public static final String ENGLISH = " abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ`1234567890-=~!@#$%^&*()_+[]{}\\|:;\"'<>,.?/";
	
	/** German specific characters */
	public static final String GERMAN = new String(new char[] { 
			'\u00c4', '\u00D6', '\u00DC', '\u00E4', '\u00F6', '\u00FC', '\u00DF' });
	
	/** French specific characters */
	public static final String FRENCH = new String(new char[] {
			'\u00C0', '\u00C2', '\u00C6', '\u00C8', '\u00C9', '\u00CA', '\u00CB', 
			'\u00CE', '\u00CF', '\u00D4', '\u0152', '\u00D9', '\u00DB', '\u00DC', 
			'\u0178', '\u00C7', '\u00E0', '\u00E2', '\u00E6', '\u00E8', '\u00E9', 
			'\u00EA', '\u00EB', '\u00EE', '\u00EF', '\u00F4', '\u0153', '\u00F9', 
			'\u00FB', '\u00FC', '\u00FF', '\u00E7' });
	
	/** Turkish specific characters */
	public static final String TURKISH = new String(new char[] { 
		    '\u00e7', '\u00c7', '\u011f', '\u011e', '\u0131', '\u0130',  
		    '\u00f6', '\u00d6', '\u015f', '\u015e', '\u00fc', '\u00dc' });

	/** characters this GLFont is created for */
	public final String alphabet;
	/** regular font height. note some special characters may not fit into this height.
	 * see {@link FontMetrics} for a discussion */
	public final int fontHeight;
	private final int baseline;
	
	private final int[] charWidths;

	public final TexturePack pack = new TexturePack();
	
	public Paint PaintObject; // Make the paint object public for later use
	
	float fontsize = -1.0f, fontpos = 0.0f;
	boolean first = true;
	int startSub = 0;
	

	/** 
	 * creates a GLFont for given awt Font consists of default characters.
	 * @see #ENGLISH 
	 */
	public AGLFont(Paint paint) {
		this(paint, ENGLISH);
		PaintObject = paint;
	}
	
	/** 
	 * creates a GLFont for given awt Font consists of characters in given alphabet 
	 * @param typeFace the awt font 
	 * @param alphabet characters of our alphabet 
	 */
	public AGLFont(Paint paint, String alphabet) {
		this.alphabet = eliminateDuplicates(alphabet);
		this.charWidths = new int[alphabet.length()];

		Bitmap.Config config = Bitmap.Config.ARGB_8888; 

		paint = new Paint(paint); 
		paint.setColor(Color.WHITE);
		
		FontMetricsInt fontMetrics = paint.getFontMetricsInt();
		
		this.fontHeight = fontMetrics.leading - fontMetrics.ascent + fontMetrics.descent;
		this.baseline = -fontMetrics.top;
		int height = fontMetrics.bottom - fontMetrics.top;

		for (int i = 0; i < alphabet.length(); i++) {
			String c = alphabet.substring(i, i + 1);
			int width = (int)paint.measureText(c);
			charWidths[i] = width;

			Bitmap charImage = Bitmap.createBitmap(width, height, config);
			Canvas canvas = new Canvas(charImage);

			canvas.drawText(c, 0, baseline, paint);
			
			pack.addImage(charImage);
		}
		pack.pack(TexturePack.ALPHA_USE);
	}

	private String eliminateDuplicates(String s) {
		StringBuilder sb = new StringBuilder(s);

		for (int i = 0; i < sb.length(); i++) {
			String c = sb.substring(i, i + 1);
			int next = -1;
			while ((next = sb.indexOf(c, i + 1)) != -1) {
				sb.deleteCharAt(next);
			}
		}
		return sb.toString();
	}

	/**
	 * returns how much area given string occupies. 
	 */
	public Rectangle getStringBounds(String s, Rectangle store) {
		if (store == null)
			store = new Rectangle();
		
		int width = 0;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int index = alphabet.indexOf(c);
			if (index == -1)
				index = alphabet.indexOf('?');
			if (index != -1) {
				width += charWidths[index];
			}
		}
		store.width = width;
		store.height = fontHeight;
		return store;
	}

	/**
	 * returns how much area given string occupies. 
	 */
	public Rectangle getStringBounds(char[] s, Rectangle store) {
		if (store == null)
			store = new Rectangle();
		
		int width = 0;

		for (int i = 0; i < s.length; i++) {
			char c = s[i];
			int index = alphabet.indexOf(c);
			if (index == -1)
				index = alphabet.indexOf('?');
			if (index != -1) {
				width += charWidths[index];
			}
		}
		store.width = width;
		store.height = fontHeight;
		return store;
	}
	
	/**
	 * blits given string to frame buffer. works very similar to
	 * awt.Graphics#drawString(..) that is: x coordinate is left most point in
	 * string, y is baseline
	 * 
	 * @param buffer
	 *            buffer to blit into
	 * @param s
	 *            string to blit
	 * @param x
	 *            leftmost point
	 * @param transparency
	 *            transparency value, make sure >= 0
	 * @param color
	 *            text color
	 * @param y
	 *            baseline
	 */
	public void blitString(FrameBuffer buffer, String s, int x, int y, int transparency, RGBColor color) {
		y -= baseline;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int index = alphabet.indexOf(c);
			if (index == -1)
				index = alphabet.indexOf('?');
			if (index != -1) {
				Point size = pack.blit(buffer, index, x, y, transparency, false, color);
				x += size.x;
			}
		}
	}
	
	public void blitString(FrameBuffer buffer, char[] s, int x, int y, int transparency, RGBColor color) {
		y -= baseline;

		for (int i = 0; i < s.length; i++) {
			char c = s[i];
			int index = alphabet.indexOf(c);
			if (index == -1)
				index = alphabet.indexOf('?');
			if (index != -1) {
				Point size = pack.blit(buffer, index, x, y, transparency, false, color);
				x += size.x;
			}
		}
	}
	
	public int getCharImageId(char c) {
		return alphabet.indexOf(c);
	}
	// Color Switch
	// return true if color changed.
	private boolean SwitchColor(String str) {
		if( str.startsWith("/#") ) {			
			if( str.equals("/#0") ) color = RGBColor.BLACK;
			if( str.equals("/#1") ) color = RGBColor.BLUE;
			if( str.equals("/#2") ) color = RGBColor.GREEN;
			if( str.equals("/#3") ) color = RGBColor.RED;
			if( str.equals("/#4") ) color = new RGBColor(255,255,0); // yellow
			if( str.equals("/#5") ) color = new RGBColor(15,5,120); // purple
			if( str.equals("/#6") ) color = new RGBColor(235,65,235); // pink 
			if( str.equals("/#7") ) color = new RGBColor(215,155,35); // orange
			if( str.equals("/#8") ) color = new RGBColor(128,128,128); // gray				
			if( str.equals("/#9") ) color = RGBColor.WHITE;
			return true;
		}		
		return false;
	}
	
	/* Made by Stephane Boivin from DevGeek Studio ( devgeek.ca / puzzker.com ) 2013
	 * Same of blitString but add special color codes, carriage return and a text width. 
	 * @param buffer
	 *            buffer to blit into
	 * @param s
	 *            string to blit
	 * @param x
	 *            leftmost point
	 * @param transparency
	 *            transparency value, make sure >= 0
	 * @param width
	 *            text size (limit on x)
	 * @param y
	 *            baseline
	 * Codes:
	 * Color code: /#0 to /#9 
	 * Carriage return: | 
	 */
	public RGBColor color = RGBColor.WHITE;	// Default color. 
	public void blitStringSpecial(FrameBuffer buffer, String s, int x, int y, int width, int transparency) {		
		char c;
		int xPos = x; // char position
		Point size;
		boolean skip = false;
		String chunk = "";
		int charSpace = 0; // Chars count before next space 
		Pattern patternSplit = Pattern.compile("[ \\|]");
		Pattern patternReplace = Pattern.compile("/#[0-9]"); // use "/#[0-9][A-Z]" to add more color code (/#A) 
		y -= baseline;
		
		for (int i = 0; i < s.length(); i++) {
			c = s.charAt(i);
			
			// Carriage return if the next chunk of string go past the bounds 
			charSpace--;
			if( charSpace <= 0 ) {
				chunk = patternSplit.split(s.substring(i),0)[0]; // get next chunk 
				charSpace = chunk.length()-1;
				chunk = patternReplace.matcher(chunk).replaceAll(""); // remove color codes
				
				// Carriage return
				if( xPos + (int)PaintObject.measureText(chunk) > x + width) {  
					xPos = x; 
					y += this.fontHeight;  
				}				
			}
						
			// Check Color code
			if( i+3 < s.length()) skip = SwitchColor(s.substring(i,i+3));
			
			if( !skip ) {				
				int index = alphabet.indexOf(c);
				if (index == -1)
					index = alphabet.indexOf('?');
				if (index != -1) {
					
					if( c == '|' ) { // carriage return 						
						xPos = x; 
						y += this.fontHeight;  
						 
					} else if ( c == '^' ) { // super script

						// Move up 2/3 of the font height
						fontpos = -0.667f*this.fontHeight;
						y += fontpos;

						// If the next character is an open group brace, flag it
						if (s.charAt(i+1) == '{')
							startSub++;
						
						
					} else if ( c == '_' ) { // sub script
												
						// Move down 2/3 of the font height
						fontpos = 0.667f*this.fontHeight;
						y += fontpos;
						
						// If the next character is an open group brace, flag it
						if (s.charAt(i+1) == '{')
							startSub++;
						
					} else if ( c == '{' ) { // skip the open group symbol
						
					} else if ( c == '}' ) { // close up on the close group symbol, but don't print it
							
						while ( startSub > 0) {
							y -= fontpos;
							startSub--;
						}
						fontpos = 0.0f;
						fontsize = -1.0f;
					} else { // blit
						size = pack.blit(buffer, index, xPos, y, transparency, false, color);
						xPos += size.x;
						
						if ( startSub == 0 && (fontpos != 0.0f || fontsize > 0.0f) ) {
							y -= fontpos;
							fontpos = 0.0f;
							fontsize = -1.0f;

						} // End if ( !startSub  && (fontpos != 0.0f || fontsize > 0.0f) )
						
					} // End blit
				}
			} else i += 2; // skip the color code
		}
	}

}
