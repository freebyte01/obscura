package obscura;

import java.awt.image.BufferedImage;

public class Map{
	public int hash, level;
	public double x,y;
	public double rot, scale=1;
	private BufferedImage img;
	public BufferedImage setImg( BufferedImage img ){ return this.img= img; }
	public BufferedImage getImg(){ return this.img; }
}
