package obscura;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

public class Utils {
	
	public static final double ratioMetric= 0.41;
	
	public static double shorter(double num){ return (double) Math.round(num*1000)/1000.000; }


	public static String join(List<String> arr, String joint){
		return join( arr.toArray(new String[arr.size()]), joint);
	}
	public static String join(String[] arr, String joint){
		if (arr.length > 0) {
		    StringBuilder sb = new StringBuilder();
		    for (String n : arr) 
		        sb.append(n).append(joint);
		    sb.deleteCharAt(sb.length() - 1);
		    return sb.toString();
		} else 
		    return ""; }
		
	public static String encodeColor(Color c){ return c.getRed()+","+c.getGreen()+","+c.getBlue()+","+c.getAlpha(); }
	public static Color decodeColor(String s){ return decodeColor( s, null ); } 
	public static Color decodeColor(String s, Color def){ 
		if (s==null) return def;
		String[] sa= s.split(","); 
		try {
			return new Color( Integer.parseInt(sa[0]), Integer.parseInt(sa[1]), Integer.parseInt(sa[2]), sa.length>3?Integer.parseInt(sa[3]):1); 
		} catch (Exception e){
			e.printStackTrace();
			return def; }
	}
	
	public static final void aaOn(Graphics2D g) { aaOn(g, true); }
	public static final void aaOn(Graphics2D g, boolean full) {
		if (full)
			g.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON );
		else
			g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR); }
	
	public static final void aaOff(Graphics2D g) {
		g.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_OFF ); }
	


	
	public static final BasicStroke textStroke = new BasicStroke(4);
	public static final void drawOutlinedString(Graphics2D g, String str, int x, int y){
		
		if (str==null || str.length()==0)
			return;
		
		g.setRenderingHint(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		FontRenderContext frc = new FontRenderContext(null,false,false);
		Font font = new Font("Dialog",Font.BOLD,20);
		FontMetrics fontMetrics = g.getFontMetrics(font);
		TextLayout tl = new TextLayout(str, font, frc);
		AffineTransform textAt = new AffineTransform();
		//textAt.translate(0, (float)tl.getBounds().getHeight());
		textAt.translate(x, y); 
		//textAt.shear(-0.5,0.0);

		Shape outline = tl.getOutline(textAt); 
		g.setColor(Color.white);
		g.setStroke(textStroke); 
		g.draw(outline);
		g.setColor(Color.black);
		g.fill(outline);

	}
	public static void drawString(Graphics2D g, double x, double y, String txt){  g.drawString( txt, (float) x, (float) y ); }
	public static Shape doEllipse(Graphics2D g, double x, double y, double w, double h, boolean fill){ return doEllipse(g, x, y, w, h, null, fill); }
	public static Shape doEllipse(Graphics2D g, double x, double y, double w, double h, Shape sh, boolean fill){
		return doShape(g, sh==null ? new Ellipse2D.Double(x,y,w,h) : sh, fill); }
	public static Shape doRectangle(Graphics2D g, double x, double y, double w, double h, boolean fill){ return doRectangle(g, x, y, w, h, null, fill); }
	public static Shape doRectangle(Graphics2D g, double x, double y, double w, double h, Shape sh, boolean fill){
		return doShape(g, sh==null ? new Rectangle2D.Double(x,y,w,h) : sh, fill); }
	public static Shape doShape(Graphics2D g, Shape sh, boolean fill){
		if (sh==null) return null;
		if (fill) g.fill(sh);
		else g.draw(sh);
		return sh; }
}
