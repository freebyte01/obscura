package obscura.parts;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

import obscura.Obscura;
import obscura.Utils;

public class ImgProvider {
	public static BufferedImage providerImgOrig; 
	public static File providerImgOrigF;
	public static BufferedImage provideImage(File imgPath) throws IOException{ return provideImage(imgPath, true, 1, 0); }
	public static BufferedImage provideImage(File imgPath, boolean genThumb) throws IOException{ return provideImage(imgPath, genThumb, 1, 0); }
	public static BufferedImage provideImage(File imgPath, int level) throws IOException{ return provideImage(imgPath, true, level, 0); }
	public static synchronized BufferedImage provideImage(File imgF, boolean genThumb, int requestLevel, int minLevel) throws IOException{
		
		if (imgF==null)
			return null;
		
		System.err.println("reading img "+ imgF + " l:"+requestLevel+ " m:"+ minLevel);
		
		String thumbsPath= ".thumbs/";
		String imgPath= imgF.getParentFile().getAbsolutePath();
		for (File o : Obscura.observeDirs)
			if (imgPath.startsWith(o.getAbsolutePath()))
				thumbsPath= o.getAbsolutePath()+"/.thumbs/";
		String thumbPath= thumbsPath+imgF.hashCode()+".jpg";
		File thumbFile= new File(thumbPath);
		String miniPath= thumbsPath+ "mini/"+imgF.hashCode()+".jpg";
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
			thumb= new BufferedImage(w, h, orig.getType());{
				Graphics2D gg= (Graphics2D) thumb.createGraphics();
				Utils.aaOn(gg, true);
				gg.drawImage(orig, 0, 0, thumb.getWidth(), thumb.getHeight(), null);
				thumbFile.getParentFile().mkdirs();
				if (genThumb) {
					System.err.println("writing thumb "+ thumbFile); 
					ImageIO.write(thumb, "jpg", thumbFile);}
			}
			System.err.println(imgF+ "("+ imgF.length()+ ")("+ (orig.getWidth()*orig.getHeight()*4)/1024+ "kb) creating thumb " + thumbFile+ "("+ thumbFile.length()+ ")("+ (thumb.getWidth()*thumb.getHeight()*4)/1024+ "kb)");
			if (!genThumb && minLevel==1)
				return thumb; }
			
		if ( minLevel==2 && genThumb && !miniFile.exists()){
			if (thumb==null)
				thumb= ImageIO.read( thumbFile );
			mini= new BufferedImage(thumb.getWidth()/4, thumb.getHeight()/4, thumb.getType());{
				Graphics2D gg= (Graphics2D) mini.createGraphics();
				Utils.aaOn(gg, true);
				gg.drawImage(thumb, 0, 0, mini.getWidth(), mini.getHeight(), null);
				if (genThumb) {
					System.err.println("writing thumb "+ thumbFile);
					ImageIO.write(mini, "jpg", miniFile); }}
			if (minLevel==2)
				return thumb; }

		if (minLevel==1)
			return thumb;

		return orig; }
	
	public static final int getOrientation(File img){
		if ( img.getName().toLowerCase().matches(".*\\.jpeg|.*\\.jpg"))
			try {
				Metadata metadata = JpegMetadataReader.readMetadata( img );
				ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType( ExifIFD0Directory.class );
				if ( exifIFD0Directory == null )
					return 1;
				try {
					return exifIFD0Directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)? exifIFD0Directory==null?1:exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION) : 1;
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
