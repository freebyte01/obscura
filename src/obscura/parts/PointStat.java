package obscura.parts;

import java.util.HashMap;

public class PointStat {
	
	public final String id;
	
	public final HashMap<String , Point> distances =  new HashMap<String, Point>();
	public final HashMap<String , Integer> counts = new HashMap<String, Integer>();
	
	public PointStat( String id ) {
		this.id= id;
	}

}
