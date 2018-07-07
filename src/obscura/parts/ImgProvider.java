package obscura.parts;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import obscura.Database;
import obscura.Obscura;
import obscura.Utils;

public class ImgProvider {
	public static BufferedImage providerImgOrig; 
	public static File providerImgOrigF;
	public static HashMap<File, Integer> orientations= new HashMap<File, Integer>(); 
	public static BufferedImage provideImage(File imgPath) throws IOException{ return provideImage(imgPath, true, 1, 0); }
	public static BufferedImage provideImage(File imgPath, boolean genThumb) throws IOException{ return provideImage(imgPath, genThumb, 1, 0); }
	public static BufferedImage provideImage(File imgPath, int level) throws IOException{ return provideImage(imgPath, true, level, 0); }
	public static String getThumbsPath(File imgF){
		String res= ".thumbs/";
		for (File o : Obscura.observeDirs)
			if (imgF.getAbsolutePath().toLowerCase().startsWith(o.getAbsolutePath().toLowerCase()))
				return o.getAbsolutePath()+"/.thumbs/";
		return res; }
	
	public static synchronized BufferedImage provideImage(File imgF, boolean genThumb, int requestLevel, int minLevel) throws IOException{
		
		if (imgF==null || !imgF.exists())
			return null;
		
		System.err.println("reading img "+ imgF + " l:"+requestLevel+ " m:"+ minLevel);
		
		String thumbsPath= getThumbsPath(imgF);
		
		String thumbPath= thumbsPath+ Database.getHashCode(imgF)+".jpg";
		File thumbFile= new File(thumbPath);
		String miniPath= thumbsPath+ "mini/"+Database.getHashCode(imgF)+".jpg";
		File miniFile= new File(miniPath);
		BufferedImage orig= null, thumb=null, mini=null;
		
		if (requestLevel==1  && thumbFile.exists())
			 return thumb= ImageIO.read( thumbFile );
		if (requestLevel==2  && miniFile.exists())
			 return mini= ImageIO.read( miniFile );

		// read original
		if ( requestLevel==0 || requestLevel==1 && !thumbFile.exists() || requestLevel==2 && !thumbFile.exists() && !miniFile.exists()){
			System.err.println("red original of "+ imgF);
			orig=  providerImgOrig= ( providerImgOrigF == imgF ? providerImgOrig : ImageIO.read( providerImgOrigF= imgF ));
		}

		if ( requestLevel==1 || genThumb && !thumbFile.exists()){
			int maxW= 1024, maxH=800;
			double iR= orig.getWidth()*1d/orig.getHeight();
			double z= iR > 1 ? maxW *1d / orig.getWidth() : maxH * 1d / orig.getHeight();
			int w= (int) Math.round( orig.getWidth()*z );
			int h= (int) Math.round( orig.getHeight()*z );
			
			thumb= new BufferedImage((int) w, (int) h, orig.getType());{
				Graphics2D gg= (Graphics2D) thumb.createGraphics();
				Utils.aaOn(gg, true);
				gg.drawImage(orig, 0, 0, thumb.getWidth(), thumb.getHeight(), null);
				if (genThumb) {
					System.err.println("writing thumb "+ thumbFile); 
					thumbFile.getParentFile().mkdirs();
					ImageIO.write(thumb, "jpg", thumbFile);}
			}
			System.err.println(imgF+ "("+ imgF.length()+ ")("+ (orig.getWidth()*orig.getHeight()*4)/1024+ "kb) creating thumb " + thumbFile+ "("+ thumbFile.length()+ ")("+ (thumb.getWidth()*thumb.getHeight()*4)/1024+ "kb)");
			if (!genThumb && minLevel==1)
				return thumb; }
			
		if ( minLevel==2 && genThumb && !miniFile.exists()){
			if (thumb==null)
				thumb= ImageIO.read( thumbFile );
			
			int w= (int) Math.round( thumb.getWidth()/4 );
			int h= (int) Math.round( thumb.getHeight()/4 );
			double rw= w, rh= h;
			
			int orientation = getOrientation(imgF);
			switch (orientation) {
			case 1:
			case 2: // Flip X
			case 3: // PI rotation
			case 4: // Flip Y
				break;
			case 5: // - PI/2 and Flip X
			case 6: // -PI/2 and -width
			case 7: // PI/2 and Flip
			case 8: // PI / 2
				rw= h;
				rh= w;
				break;
			default:
				break;
			}
			
			mini= new BufferedImage((int)rw, (int)rh, thumb.getType());
			if (mini!=null){
				Graphics2D gg= (Graphics2D) mini.createGraphics();
				Utils.aaOn(gg, true);
				gg.translate( rw/2, rh/2 );

				gg.setTransform( ImgProvider.getAffineForOrientation(gg.getTransform(), orientation, (int) Math.round(w), (int) Math.round(h)) );
				gg.drawImage(thumb, 0, 0, (int)w, (int)h, null);
				if (genThumb) {
					System.err.println("writing mini thumb "+ thumbFile);
					miniFile.getParentFile().mkdirs();
					ImageIO.write(mini, "jpg", miniFile); }}
			if (minLevel==2)
				return mini; }

		if (minLevel==1)
			return thumb;

		return orig; }
	
	public static final int getOrientation(File imgF){
		if (orientations.containsKey(imgF))
			return orientations.get(imgF);
		if ( imgF.getName().toLowerCase().matches(".*\\.jpeg|.*\\.jpg"))
			try {
				Metadata metadata = JpegMetadataReader.readMetadata( imgF );
				ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType( ExifIFD0Directory.class );
				if ( exifIFD0Directory == null )
					return 1;
				try {
					int orientation= exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)? exifIFD0Directory==null?1:exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION) : 1;
					orientations.put(imgF, orientation);
					return orientation;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} catch (JpegProcessingException e) {
				System.err.println(e);
			} catch (IOException e) {
				System.err.println(e);
			}
		return 1;
	}
	
	public static final AffineTransform getAffineForOrientation(AffineTransform current, int orientation, int width, int height){
		AffineTransform affineTransform = current;
		switch (orientation) {
		case 1:
			affineTransform.translate(-width/2, -height/2);
			break;
		case 2: // Flip X
			affineTransform.scale(-1.0, 1.0);
			affineTransform.translate(-width, 0);
			break;
		case 3: // PI rotation
			affineTransform.translate(width, height);
			affineTransform.rotate(Math.PI);
			affineTransform.translate(width/2, height/2);
			break;
		case 4: // Flip Y
			affineTransform.scale(1.0, -1.0);
			affineTransform.translate(-width/2, -height/2);
			break;
		case 5: // - PI/2 and Flip X
			affineTransform.rotate(-Math.PI / 2);
			affineTransform.scale(-1.0, 1.0);
			affineTransform.translate(-width/2, -height/2);
			break;
		case 6: // -PI/2 and -width
			affineTransform.translate(height, 0);
			affineTransform.rotate(Math.PI / 2);
			affineTransform.translate(-width/2, height/2);
			break;
		case 7: // PI/2 and Flip
			affineTransform.scale(-1.0, 1.0);
			affineTransform.translate(-height, width);
			affineTransform.rotate(3 * Math.PI / 2);
			affineTransform.translate(width/2, height/2);
			break;
		case 8: // PI / 2
			affineTransform.translate(0, width);
			affineTransform.rotate(3 * Math.PI / 2);
			affineTransform.translate(width/2, -height/2);
			break;
		default:
			break;
		}       
		return affineTransform;
	}

}
