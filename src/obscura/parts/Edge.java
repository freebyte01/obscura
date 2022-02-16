package obscura.parts;

import java.awt.Color;

import obscura.Database;
import obscura.Utils;

public class Edge{
	
	public Similarity a, b;
	public String ia, ib;
	public Color clr = Color.white;
	public double w = 1;
	
	public Edge() {}
	
	public Edge( String def ) {
		if ( def !=null )
			read( def ); }
	
	
	public Edge( Similarity a, Similarity b ) { 
		this.ia = (this.a = a).id; 
		this.ib = (this.b = b).id; }
	
	
	public Edge set( Similarity a, Similarity b ) { 
		this.ia = (this.a = a).id; 
		this.ib = (this.b = b).id;
		return this; }
	
	
	public String serialize() { 
		return ia + "/" + ib; }

	
	Similarity getA() { return a = Database.similarities.get( ia ); }
	Similarity getB() { return b = Database.similarities.get( ib ); }
	
	
	public String toString() { 
		return "[ "+ ia + ":"+ ib+ " ]"; }
	

	public String store(){
		StringBuilder def= new StringBuilder( "edge;" );
		if ( a != null ) def.append( "a:"+ ia + ";" );
		if ( b != null ) def.append( "b:"+ ib + ";" );
		if ( w != 1 ) def.append( "w:"+ w + ";" );
		if ( clr != Color.white ) def.append( "c:"+ Utils.encodeColor( clr ) + ";" );
		def.append( "\n" );
		return def.toString(); }

	
	public void read( String definition ){
		if ( !definition.startsWith( "edge;" ))
			return;
		ia= Database.getValue( definition, "a", "" );
		ib= Database.getValue( definition, "b", "" ); 
		clr= Utils.decodeColor( Database.getValue( definition, "c", null ), Color.white ); 
		w= Double.parseDouble( (String) Database.getValue( definition, "w", "1" )); }
	
}