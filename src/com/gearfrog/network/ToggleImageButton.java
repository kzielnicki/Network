package com.gearfrog.network;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.ImageButton;

public class ToggleImageButton extends ImageButton {
     // ===========================================================
     // Fields
     // ===========================================================

     protected boolean isChecked = false;

     // ===========================================================
     // Constructors
     // ===========================================================
     public ToggleImageButton(Context context) {
          super(context);
     }
     
     public ToggleImageButton(Context context, AttributeSet attrs)
     {
         super(context, attrs);
     }
     
    public ToggleImageButton(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

     // ===========================================================
     // Getter & Setter
     // ===========================================================
     public boolean isChecked() {
          return this.isChecked;
     }
     
     public void setChecked(boolean check) {
    	 isChecked = check;
    	 refreshDrawableState();
     }

     // ===========================================================
     // Methods
     // ===========================================================

     @Override
     public boolean performClick() {
          this.isChecked = !this.isChecked;
          return super.performClick();
     }

     /** Return an array of resource IDs of
      * the Drawable states representing the
      * current state of the view. */
     @Override
     public int[] onCreateDrawableState(int extraSpace) {
          int[] states;
          if (this.isChecked()) {
               // Checked
               states = Button.PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
          } else {
               // Unchecked
               states = super.onCreateDrawableState(extraSpace);
          }
          return states;
     }
}