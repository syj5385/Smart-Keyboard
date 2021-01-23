package jjun.smartkeyboard.CustomAdapter3;

import android.graphics.drawable.Drawable;

/**
 * Created by jjunj on 2016-12-20.
 */

public class Custom3_Item {

    private Drawable mIcon;
    private String[] mData;

    public Custom3_Item(String obj){
        mData[0] = obj;
    }

    public Custom3_Item(Drawable icon, String[] obj){
        mIcon = icon;
        mData = obj;
    }

    public Custom3_Item(Drawable icon, String obj01, String obj02){
        mIcon = icon;

        mData = new String[2];
        if(obj01 !=  null) {
            mData[0] = obj01;
        }
        else{
            mData[0] = "Unknown";
        }
        mData[1] = obj02;
    }

    public String[] getData(){
        return mData;
    }

    public String getData(int index){
        if(mData == null || index >= mData.length){
            return null;
        }

        return mData[index];
    }

    public void setData(String[] obj){
        mData = obj;
    }

    public void setIcon(Drawable icon){
        mIcon = icon;
    }

    public Drawable getIcon(){
        return mIcon;
    }
}