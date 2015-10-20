package slideshow;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends Application {

    /////////////////
    // Application //
    /////////////////

    public static void main(String[] args) {
        if ( args.length>0 ) {
            startFolder = args[0];
            if ( args.length>1 ) {
                if ( "random".equalsIgnoreCase(args[1]) ) {
                    // random not yet implemented
                } else {
                    try {
                        setSize = Integer.parseInt(args[1]);
                    } catch ( NumberFormatException ignored) {}
                }
                if ( args.length>2 ) {
                    try {
                        imageDuration = Integer.parseInt(args[2]) * 1000;
                    } catch ( NumberFormatException ignored) {}
                }
            }
            System.out.println("=====================================================");
            System.out.println("  Slide Show\n    " + startFolder + "\n");
            System.out.println("  Set Size: " + setSize);
            System.out.println("  Delay: " + imageDuration + "ms");
            System.out.println("=====================================================");
            launch(args);
        }
    }

    private static String startFolder;
    private static long imageDuration = 8000;
    private static int setSize = 5;


    ////////
    // UI //
    ////////

    @Override
    public void start(Stage stage) throws Exception{

        startSlideShow();

        setupCanvas(stage);

        new Timer(true).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        nextImage();
                    }
                }, 0, imageDuration
        );

    }

    private void setupCanvas(Stage stage) {

        width = (int)Math.floor(Screen.getPrimary().getVisualBounds().getWidth());
        height = (int)Math.floor(Screen.getPrimary().getVisualBounds().getHeight()) + 50;

        final Pane pane = new Pane();
        Group board = new Group();
        pane.getChildren().add(board);

        stage.setScene(new Scene(pane, width, height));
        stage.setFullScreen(true);
        stage.setTitle("Slideshow");
        stage.show();

        Canvas canvas = new Canvas(width,height);
        board.getChildren().add(canvas);

        gc = canvas.getGraphicsContext2D();

        pane.setOnMouseClicked(event -> {
            if ( event.isControlDown() ) {
                paused = !paused;
                System.out.println(paused ? "|Pause|" : ">Resume>");
            }
        });

    }

    private void drawImage(String filename) {

        gc.fillRect(0, 0, width, height);

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(filename));
        } catch (IOException ignored) {}

        image = Scalr.resize(image, Scalr.Mode.FIT_TO_HEIGHT, width, height);
        if ( image.getWidth() > width ) {
            image = Scalr.resize(image, Scalr.Mode.FIT_TO_WIDTH, width);
        }

        Image img = SwingFXUtils.toFXImage(image, null);
        gc.drawImage(img, (width-image.getWidth())/2, 0);

    }

    private GraphicsContext gc;

    private int width = 1700;
    private int height = 2000;


    ////////////////
    // Slide Show //
    ////////////////

    private void startSlideShow() {
        buildImageList(new File(startFolder));
        thinImageList();
        setIterator = allImageFilenames.keySet().iterator();
    }

    private void nextImage() {
        if ( !paused ) {
            if ( imageIterator==null ) {
                if ( !setIterator.hasNext() ) {
                    startSlideShow();
                }
                imageIterator  = allImageFilenames.get(setIterator.next()).iterator();
            }
            if ( imageIterator.hasNext() ) {
                drawImage(imageIterator.next());
            } else {
                imageIterator = null;
                nextImage();
            }
        }
    }


    private void buildImageList(File dir) {

        String dirName = dir.getName();
        File[] files = dir.listFiles();

        if ( files!=null ) {

            Arrays.sort(files);

            for (File file : files) {

                if ( file.isDirectory() ) {
                    buildImageList(file);
                } else {
                    if ( isImage(file) ) {
                        List<String> filenames = allImageFilenames.get(dirName);
                        if ( filenames==null ) {
                            filenames = new LinkedList<>();
                            allImageFilenames.put(dirName, filenames);
                        }
                        filenames.add(file.getPath());
                    }
                }
            }

        }
    }

    private void thinImageList() {
        Random random = new Random(System.currentTimeMillis());
        for (Map.Entry<String, List<String>> entry : allImageFilenames.entrySet() ) {
            List<String> filenames = entry.getValue();
            List<String> thinned = new LinkedList<>();
            int i = 0;
            int limit = Math.min(filenames.size(), setSize);
            while ( i < limit ) {
                int r = (int)Math.floor(random.nextDouble() * filenames.size());
                String f = filenames.get(r);
                if ( !thinned.contains(f) ) {
                    thinned.add(f);
                    i++;
                }
            }
            Collections.sort(thinned);
            allImageFilenames.put(entry.getKey(), thinned);
        }
    }

    private boolean isImage(File file) {
        if ( file.getName().length()>=4 ) {
            String ext = file.getName().substring(file.getName().length() - 3);
            return ext.equalsIgnoreCase("jpg")
                    || ext.equalsIgnoreCase("peg")
                    || ext.equalsIgnoreCase("png")
                    || ext.equalsIgnoreCase("gif")
                    || ext.equalsIgnoreCase("bmp");
        }
        return false;
    }

    private void printFileList() {
        for (Map.Entry<String, List<String>> entry : allImageFilenames.entrySet() ) {
            System.out.println(entry.getKey());
            for ( String s : entry.getValue() ) {
                System.out.println("\t" + s);
            }
        }
    }

    private Map<String, List<String>> allImageFilenames = new LinkedHashMap<>();

    private Iterator<String> setIterator = null;
    private Iterator<String> imageIterator = null;

    private boolean paused = false;

}
