package com.gearfrog.network;

import android.os.Parcel;
import android.os.Parcelable;

public class Player implements Parcelable {
	public String name;
	public int color;
	
	public Player(String n, int c) {
		name = n;
		color = c;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeInt(color);
	}

	public static final Parcelable.Creator<Player> CREATOR
	= new Parcelable.Creator<Player>() {
		public Player createFromParcel(Parcel in) {
			return new Player(in);
		}

		public Player[] newArray(int size) {
			return new Player[size];
		}
	};

    private Player(Parcel in) {
        name = in.readString();
        color = in.readInt();
    }
}
