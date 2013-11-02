import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;

public class BlobDetectionStep extends Step {
    private BufferedImage baseImage;
    // private BufferedImage processedImage;
    // private BufferedImage displayImage;
    private int[][] blobs;
    public static final int BLOB_THRESHOLD = 0;
    BufferedImage processedImage;

    public BlobDetectionStep(Listener listener) {
        super(listener);

        setFocusable(true);

    }

    public void begin(Object input) {
        baseImage = (BufferedImage) input;
        baseImage = Utility.addAlphaChannel(baseImage);
        setPreferredSize(new Dimension(baseImage.getWidth(), baseImage.getHeight()));
        blobs = new int[baseImage.getWidth()][baseImage.getHeight()];
        ArrayList<Piece> output = detectBlobs();
        System.out.println("finished");
        System.out.println("Total Blobs Found: " + output.size());
        listener.update(this, output);
    }

    @Override
    public void paint(Graphics g) {
        Utility.drawChecker(g, getWidth(), getHeight(), 10, Color.LIGHT_GRAY, Color.DARK_GRAY);
        if (processedImage != null) {
            g.drawImage(processedImage, 0, 0, processedImage.getWidth(), processedImage.getHeight(), null);
        }
    }

    public ArrayList<Piece> detectBlobs() {
        int blobNum = 0;

        ArrayList<BlobRegion> blobRegions = new ArrayList<BlobRegion>();
        Stack<Point> pixelStack = new Stack<Point>();
        for (int x = 0; x < baseImage.getWidth() - 1; x++) {
            for (int y = 0; y < baseImage.getHeight() - 1; y++) {
                if (blobs[x][y] != 0) {
                    continue;
                }
                if (getAlphaValue(baseImage.getRGB(x, y)) <= BLOB_THRESHOLD) {
                    continue;
                }
                /*
                 * For every pixel not part of a blob Add the pixel to a stack While the stack isn't empty, pop off the pixel, mark it as a blob, add its non-transparent neighbors
                 */
                pixelStack.push(new Point(x, y));

                blobNum++;
                BlobRegion blobRegion = new BlobRegion(blobNum);
                blobRegion.minX = x;
                blobRegion.minY = y;
                blobRegion.maxX = x;
                blobRegion.maxY = y;
                blobRegions.add(blobRegion);
                while (!pixelStack.empty()) {
                    Point currentPixel = pixelStack.pop();
                    // System.out.printf("Popped: %d %d\n", currentPixel.x, currentPixel.y);
                    blobs[currentPixel.x][currentPixel.y] = blobNum;
                    if (currentPixel.x < blobRegion.minX) {
                        blobRegion.minX = currentPixel.x;
                    }
                    if (currentPixel.y < blobRegion.minY) {
                        blobRegion.minY = currentPixel.y;
                    }
                    if (currentPixel.x > blobRegion.maxX) {
                        blobRegion.maxX = currentPixel.x;
                    }
                    if (currentPixel.y > blobRegion.maxY) {
                        blobRegion.maxY = currentPixel.y;
                    }
                    // System.out.println(getAlphaValue(baseImage.getRGB(currentPixel.x, currentPixel.y)));
                    for (int i = currentPixel.x - 1; i <= currentPixel.x + 1; i++) {
                        for (int j = currentPixel.y - 1; j <= currentPixel.y + 1; j++) {
                            // System.out.printf("dsa: %d %d\n", i, j);
                            if (i < 0 || j < 0 || i >= baseImage.getWidth() || j >= baseImage.getHeight()) {
                                continue;
                            }
                            // System.out.println("dsaf");
                            if (blobs[i][j] != 0) {
                                // System.out.println((i == currentPixel.x) + " " + (j == currentPixel.y) + " " + getAlphaValue(baseImage.getRGB(i, j)));
                                continue;
                            }

                            if (getAlphaValue(baseImage.getRGB(i, j)) <= BLOB_THRESHOLD) {
                                continue;
                            }

                            // System.out.printf("Pushing: %d %d\n", i, j);
                            pixelStack.push(new Point(i, j));
                        }
                    }
                    // System.exit(1);
                }

                /*
                 * Iterative Method, creates too many blobs // Check neighbors // If neighbor is not transparent, set to highest neighbor number // If neighbors == transparent, set own blob color int highestNeighbor = 0; for (int i = x - 1; i < x + 1; i++) { for (int j = y - 1; j < y + 1; j++) { if (blobs[i][j] > highestNeighbor) { highestNeighbor = blobs[i][j]; } } } if (highestNeighbor > 0) { blobs[x][y] = highestNeighbor; } else { blobNum++; blobs[x][y] = blobNum; }
                 */

                /*
                 * //Recursive (stack overflow) if (blobs[x][y] != 0) continue; if (getAlphaValue(baseImage.getRGB(x, y)) <= BLOB_THRESHOLD) continue; System.out.println(x + " " + y); fillBlob(x, y, blobNum); blobNum++;
                 */
            }
        }
        System.out.println((blobNum) + " blobs found");
        int[] blobColors = new int[blobNum + 1];
        for (int i = 0; i < blobColors.length; i++) {
            Color randomColor = new Color((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
            blobColors[i] = randomColor.getRGB();
        }
        System.out.println("Creating outImage");
        BufferedImage outImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < baseImage.getWidth(); x++) {
            for (int y = 0; y < baseImage.getHeight(); y++) {
                if (blobs[x][y] == 0) {
                    outImage.setRGB(x, y, 0x00FFFFFF);
                } else {
                    outImage.setRGB(x, y, blobColors[blobs[x][y]]);
                }
                /*
                 * if (blobs[x][y] == 1) { System.out.printf("x: %d, y: %d\n", x, y); }
                 */
            }
        }
        ArrayList<Piece> blobList = new ArrayList<Piece>();
        for (BlobRegion region : blobRegions) {
            BufferedImage regionImage = new BufferedImage(region.maxX - region.minX + 1, region.maxY - region.minY + 1, BufferedImage.TYPE_INT_ARGB);
            for (int x = region.minX; x <= region.maxX; x++) {
                for (int y = region.minY; y <= region.maxY; y++) {
                    if (blobs[x][y] != region.blobNum) {
                        regionImage.setRGB(x - region.minX, y - region.minY, 0x00FFFFFF);
                    } else {
                        regionImage.setRGB(x - region.minX, y - region.minY, baseImage.getRGB(x, y));
                    }
                }
            }
            Point2D.Double pos = new Point2D.Double(region.minX + (regionImage.getWidth() / 2.0), region.minY + (regionImage.getWidth() / 2.0));
            Piece p = new Piece(pos, 0, regionImage);
            blobList.add(p);
        }

        processedImage = outImage;
        repaint();
        return blobList;
    }

    public int getAlphaValue(int pixel) {
        return (pixel >> 24) & 0xFF;
    }
}
