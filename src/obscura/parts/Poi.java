package obscura.parts;

import java.awt.Color;

import obscura.Database;
import obscura.Obscura;
import obscura.Utils;

public class Poi{
	
	public String name;
	public Color clr = Color.white;
	public String id = "";
	
	public Poi() {}
	
	public Poi( String def ) {
		if ( def !=null )
			read( def ); }
	
	
	public Poi( String nm, String id ) { 
		this.name = nm;
		this.id = id; }
	
	
	public Poi set( String name ) { 
		this.name = name;
		return this; }
	
	
	public String serialize() { 
		return name + "/" + id; }

	
	public String toString() { 
		return "[ "+ name + ":"+ id + " ]"; }
	

	public String store(){
		StringBuilder def= new StringBuilder( "poi;" );
		if ( name != null ) def.append( "nm:"+ name + ";" );
		if ( id != "" ) def.append( "id:"+ id + ";" );
		def.append( "\n" );
		return def.toString(); }

	
	public void read( String definition ){
		if ( !definition.startsWith( "poi;" ))
			return;
		name= Database.getValue( definition, "nm", "" );
		clr= Utils.decodeColor( Database.getValue( definition, "c", null ), Color.white ); 
		id= (String) Database.getValue( definition, "id", "" );
		Database.POIs.put( name, this );
		Database.POIss.put( id, this ); }
	
	
	public void rename( String newName ) {
		
		if ( Database.POIs.get(newName) != null
		&& Database.POIs.get(newName) != this ){
			System.err.println("cant rename as the POI name already exists!");
			return; }
		
	
		for ( ImgDef def : Database.imgInfos.values()){
			Point poiP = def.POIs.remove( name );
		if ( poiP != null )
			def.POIs.put( newName, poiP ); }
	
	for ( Similarity sim : Database.similarities.values())
		if ( sim.POIs.contains( name )){
			if ( !sim.POIs.contains( newName ))
				sim.POIs.add( newName );
			sim.POIs.remove( name ); }
	
	Poi rpoi = Database.POIs.remove( name );
	
	name = newName;
	
	Database.POIs.put( newName, rpoi );
	Database.sortPOIs();
	Obscura.data.writeDatabase(); }
	
}