package obscura.parts;

import java.awt.Color;

import obscura.Database;
import obscura.Obscura;
import obscura.Utils;

public class Place{
	Area assoc;
	double relX, relY;
	Color clr= Color.yellow;
	String id, pid, label;
	
	
	public Place(Area a) {
		assoc= a;
		pid= a.id;
		Obscura.data.writeDatabase();
	}
	
	public Place(String def) {
		read(def);
	}
	
	void read(String definition){
		if (!definition.startsWith("place;"))
			return;
		id= Database.getValue(definition, "id", null);
		pid= Database.getValue(definition, "pid", null);
		label= Database.getValue(definition, "label", null);
		relX= Double.parseDouble(Database.getValue(definition, "rx", "0"));
		relY= Double.parseDouble(Database.getValue(definition, "ry", "0"));
		clr= Utils.decodeColor(Database.getValue(definition, "clr", null), Color.white); 
	
		if (Database.areas.containsKey(pid))
			Database.areas.get(pid).places.add(this);

	}
	
	String store(String parentId){
		StringBuilder def= new StringBuilder("place;");
		def.append( "pid:"+ parentId+ ";");
		if (label!=null) def.append( "label:"+ label+ ";");
		if (relX!=0) def.append( "rx:"+ relX+ ";");
		if (relY!=0) def.append( "ry:"+ relY+ ";");
		def.append( "clr:"+ Utils.encodeColor(clr)+ ";");
		def.append("\n");
		return def.toString();
	}
}