import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Path2D;

public class LineWithArrow extends JPanel {
  private double  cx = 200, cy = 200, mx = 100, my = 100;

  LineWithArrow () {
    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged (MouseEvent e) {
        mx = e.getX();
        my = e.getY();
        repaint();
      }
    });
    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed (MouseEvent e) {
        mx = e.getX();
        my = e.getY();
        repaint();
      }
    });
  }

  // Get line shape with arrow at end point (y2, y2)
  static Path2D.Double lineWithArrow (double x1, double y1, double x2, double y2) {
    Path2D.Double path = new Path2D.Double();
    path.moveTo(x1, y1);
    path.lineTo(x2, y2);
    double angle = Math.atan2(y2 - y1, x2 - x1);
    double angleOff = Math.toRadians(20);
    int barb = 10;
    double x = x2 - barb * Math.cos(angle - angleOff);
    double y = y2 - barb * Math.sin(angle - angleOff);
    path.moveTo(x, y);
    path.lineTo(x2, y2);
    x = x2 - barb * Math.cos(angle + angleOff);
    y = y2 - barb * Math.sin(angle + angleOff);
    path.moveTo(x, y);
    path.lineTo(x2, y2);
    return path;
  }

  // Get line shape with arrow at both start and end points
  static Path2D.Double lineWithArrows (double x1, double y1, double x2, double y2) {
    Path2D.Double path = lineWithArrow(x1, y1, x2, y2);
    double angle = Math.atan2(y1 - y2, x1 - x2);
    double angleOff = Math.toRadians(20);
    int barb = 10;
    double x = x1 - barb * Math.cos(angle - angleOff);
    double y = y1 - barb * Math.sin(angle - angleOff);
    path.moveTo(x, y);
    path.lineTo(x1, y1);
    x = x1 - barb * Math.cos(angle + angleOff);
    y = y1 - barb * Math.sin(angle + angleOff);
    path.moveTo(x, y);
    path.lineTo(x1, y1);
    return path;
  }

  // Get line shape with arrow at end point (p2)
  static Path2D.Double lineWithArrow (Point.Double p1, Point.Double p2) {
    return lineWithArrow(p1.x, p1.y, p2.x, p2.y);
  }

  // Get line shape with arrow at both start and end points
  static Path2D.Double lineWithArrows (Point.Double p1, Point.Double p2) {
    return lineWithArrows(p1.x, p1.y, p2.x, p2.y);
  }

  @Override
  protected void paintComponent (Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setPaint(Color.blue);
    g2.draw(lineWithArrow(cx, cx, mx, my));
  }

  public static void main (String[] args) {
    LineWithArrow test = new LineWithArrow();
    JFrame f = new JFrame();
    f.add(test);
    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    f.setSize(400, 400);
    f.setLocationRelativeTo(null);
    f.setVisible(true);
  }
}