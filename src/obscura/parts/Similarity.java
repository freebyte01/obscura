package obscura.parts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import obscura.Database;
import obscura.Obscura;
import obscura.Utils;

public class Similarity {
	
	public String id;
	public String label;
	public final LinkedList<String> register = new LinkedList<String>();

	int feature = 0;
	static final int NONE = 10;
	static final int TARGET = 10;
	static final int ORIGIN = 20;
	static final int VIEW = 30;
	
	public LinkedList<String> POIs = new LinkedList<String>();

	public Similarity() {}

	public Similarity( String id ) {
		this.id = id;
		Database.similarities.put( this.id, this ); }
	
	
	public Similarity registerKey( String defKey ){
		return registerKey( defKey, true ); }
	
	
	public Similarity registerKey( String defKey, boolean storeImmediately ){
		
		if ( !register.contains( defKey ))
			register.add( defKey );
		
		ImgDef def= Database.imgInfos.get( defKey );
		
		if ( def!=null ) 
			def.similar= this;
		
		if ( storeImmediately )
			Obscura.data.writeDatabase();
		
		lastModificationTime= System.currentTimeMillis();
		
		return this; }
	
	
	public String store(){
		StringBuilder def= new StringBuilder( "simil;" );
		def.append( "id:"+ id+ ";" );
		def.append( "register:"+ Utils.join( register, "," )+ ";" );
		
		if ( POIs.size()>0 ){
			
			StringBuilder sb= new StringBuilder( "pois:" );
			for ( String poi : POIs )
				sb.append( poi+"," );
			def.append( sb.toString().substring( 0, sb.length()-1 )+";" ); }
		
		return def.toString()+"\n"; }
	
	
	public void read( String definition ){
		
		if ( !definition.startsWith( "simil;" ))
			return;
		
		this.id= Database.getValue( definition, "id", System.currentTimeMillis()+"" );
		String pois= Database.getValue( definition,"pois", "" );
		
		if ( pois.length()>0 )
			for ( String poi : pois.split( "," )){
				Database.addPOI( poi ); 
				POIs.add( poi ); }
		
		register.clear();
		register.addAll( Arrays.asList( Database.getValue( definition, "register", "" ).split( "," )));
		
		for( String key : register ){
			ImgDef def= Database.imgInfos.get( key );
			if ( def!=null )
				def.similar= this; }		
		
		Database.similarities.put( this.id, this ); }

	
	long lastSort=-1; 
	public long lastModificationTime=System.currentTimeMillis();
	
	public void sort(){
		if ( lastSort>=lastModificationTime )
			return;
		ArrayList<ImgDef> defs= new ArrayList<ImgDef>();
		ArrayList<String> nonDef= new ArrayList<String>();
		
		for( String key : register ){
			ImgDef def= Database.imgInfos.get( key );
			if ( def!=null && def.file!=null )
				defs.add( def );
			else // to not loose those which have not def available yet
				nonDef.add( key ); }
		
		ImgDef[] sort= new ImgDef[ defs.size()];
		defs.toArray( sort );
		
		Arrays.sort( sort, new Comparator<ImgDef>() {
	    	public int compare( ImgDef o1, ImgDef o2 ) {
    			return o1.file == null ? o2.file == null ? 0 : -1 : o2.file==null ? 1 : o1.file.lastModified() >= o2.file.lastModified() ? 1 : -1; 
    		}} );
		
		register.clear();
		
		for ( ImgDef def : sort ){
			register.add( def.getKey());
			System.err.println( ">>"+ def.file.lastModified()); }
		
		register.addAll( nonDef );
		System.err.println( "sorted "+ sort.length );
		lastSort= lastModificationTime; }
	
	
	public Point[] getPositioning(){
		Point pos= new Point( 0,0 ); int posCnt= 0;
		Point targ= new Point( 0,0 ); int targCnt= 0;
		for( String key : register ){
			ImgDef def= Database.imgInfos.get( key );
			if ( def!=null ){
				if ( def.pos!=null ){
					pos.add( def.pos );
					posCnt++; }
				if ( def.targ!=null ){
					targ.add( def.targ );
					targCnt++; }}}
		return new Point[]{ posCnt==0 ? pos : pos.mul( 1d/posCnt ), targCnt==0 ? targ : targ.mul( 1d/posCnt ) }; }
}
