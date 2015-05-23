package ksami.gpsservicetest;

public class NewPrediction {
	int hour;
	int day_of_week;
	int fut_grid_x;
	int fut_grid_y;
	String fut_area_name;

	NewPrediction(){
		hour = -1;
		day_of_week = -1;
		fut_grid_x = -1;
		fut_grid_y = -1;
		fut_area_name = null;
	}

	NewPrediction(int hour, int day_of_week, int fut_grid_x, int fut_grid_y, String fut_area_name){
		this.hour = hour;
		this.day_of_week = day_of_week;
		this.fut_grid_x = fut_grid_x;
		this.fut_grid_y = fut_grid_y;
		this.fut_area_name = fut_area_name;
	}
}