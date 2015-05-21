package com.example.cse.myapplication;

import java.util.Date;

public class UserPos{
	public Date date;
	public int time;
	public int day;
	public int x;
	public int y;
	
	UserPos(){
		date = null;
		time = -1;
		day = -1;
		x = -1;
		y = -1;
	}
	
	UserPos(Date date, int time, int day, int x, int y){
		this.date = date;
		this.time = time;
		this.day = day;
		this.x = x;
		this.y = y;
	}
}