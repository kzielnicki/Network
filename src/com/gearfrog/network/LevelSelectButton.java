package com.gearfrog.network;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class LevelSelectButton extends Button {
     // ===========================================================
     // Fields
     // ===========================================================

     protected int level = 1;

     // ===========================================================
     // Constructors
     // ===========================================================
     public LevelSelectButton(Context context) {
          super(context);
     }
     
     public LevelSelectButton(Context context, AttributeSet attrs)
     {
         super(context, attrs);
     }
     
    public LevelSelectButton(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

     // ===========================================================
     // Getter & Setter
     // ===========================================================
     public int getLevel() {
          return level;
     }
     
     public void setLevel(int l) {
    	 level = l;
         this.setText("Level "+level);
     }

     // ===========================================================
     // Methods
     // ===========================================================

     @Override
     public boolean performClick() {
          level++;
          if(level > 3)
        	  level = 1;
          this.setText("Level "+level);
          return super.performClick();
     }

}