package com.gearfrog.network;

import android.graphics.Color;
import android.util.Log;

public class ColorHelper {
	public static int setAlpha(int c, int alpha) {
		return Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c));
	}
	
	// interpolate from c1 to c2 by a fraction f, which must be between 0 and 1
	public static int interpolate(int c1, int c2, float f) {
		int alpha = (int)((Color.alpha(c2)-Color.alpha(c1))*f)+Color.alpha(c1);
		int red = (int)((Color.red(c2)-Color.red(c1))*f)+Color.red(c1);
		int green = (int)((Color.green(c2)-Color.green(c1))*f)+Color.green(c1);
		int blue = (int)((Color.blue(c2)-Color.blue(c1))*f)+Color.blue(c1);
		
		//Log.v("ColorHelper","alpha = "+alpha);
		return Color.argb(alpha, red, green, blue);
	}

}
