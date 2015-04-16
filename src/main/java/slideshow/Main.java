package slideshow;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends Application {

    private long showImageFor = 7000;
    private long imageAdvanceAfter = 20000;
    private long folderAdvanceAfter = 50000;

    private int maxFolderIndex = 0;
    private int minFolderIndex = 0;

    private int width = 1700;
    private int height = 2000;

    private Map<Integer, Map<Integer, String>> allImages = new HashMap<>();

    private Map<Integer, Double> maxImageFactors = new HashMap<>();
    private Map<Integer, Double> minImageFactors = new HashMap<>();

    private GraphicsContext gc;
    private Group board;

    private static String startFolder;

    @Override
    public void start(Stage stage) throws Exception{

        loadImages(new File(startFolder));

        final Pane pane = new Pane();
        board = new Group();
        pane.getChildren().add(board);

        stage.setScene(new Scene(pane, width, height));
        stage.setFullScreen(true);
        stage.setTitle("Slideshow");
        stage.show();

        Canvas canvas = new Canvas(width,height);
        gc = canvas.getGraphicsContext2D();
        gc.fillRect(10,10,10,10);

        board.getChildren().add(canvas);

        new Timer(true).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        putRandomImages(2);
                    }
                }, 0, showImageFor
        );

        new Timer(true).schedule(
                new TimerTask() {
                    @Override public void run()
                    {
                        advanceImage();
                    }
                }, imageAdvanceAfter, imageAdvanceAfter
        );

        new Timer(true).schedule(
                new TimerTask() {
                    @Override public void run()
                    {
                        advanceFolder();
                    }
                }, folderAdvanceAfter, folderAdvanceAfter
        );

        pane.setOnMouseClicked(event -> paused = !paused);
    }

    private boolean paused = false;

    private void advanceFolder() {
//        System.out.println("Folder++");
        if ( !paused ) {
            if (maxFolderIndex < allImages.keySet().size()) {
                maxFolderIndex++;
            } else {
                if (minFolderIndex < allImages.keySet().size()) {
                    minFolderIndex++;
                } else {
                    minFolderIndex = 0;
                    maxFolderIndex = 0;
                }
            }
        }
    }

    private void advanceImage() {
        if ( !paused ) {
            for (int i = 0; i <= maxFolderIndex; i++) {
                if (i < minImageFactors.size()) {
                    double min = minImageFactors.get(i);
                    double max = maxImageFactors.get(i);
                    int imageCount = allImages.get(i).size();
                    if ( imageCount==0 ) {
//                        advanceFolder();
                    } else {
                        if (max < 1.0) {
                            maxImageFactors.put(i, max + 0.2);
                        } else {
                            if (min < 0.9) {
                                minImageFactors.put(i, min + 0.2);
                            } else {
                                minImageFactors.put(i, 0.0);
                                maxImageFactors.put(i, 0.2);
                            }
                        }
                    }
                }
            }
        }
//        System.out.println("Settings:");
//        for ( int i=0; i<allImages.keySet().size(); i++ ) {
//            Double min = minImageFactors.get(i);
//            Double max = maxImageFactors.get(i);
//            if ( min!=null && max!=null ) {
//                int iMin = (int)Math.floor(min*10);
//                int iMax = (int)Math.floor(max*10);
//                System.out.println(i + ": " + iMin + "-" + iMax + (i == maxFolderIndex ? " *" : ""));
//            }
//        }
//        System.out.println();
    }


    private void putRandomImages(int count) {
        if ( !paused ) {

            int left = 0;
            gc.fillRect(0, 0, width, height);
            int i = 0;
            int retries = 0;
            List<BufferedImage> nowImages = new ArrayList<>();
            while (i < count) {
                String filename = getRandomImage();

                if (filename != null) {
                    BufferedImage image = null;
                    try {
                        image = ImageIO.read(new File(filename));
                        filesDisplayed.add(filename);
                    } catch (IOException e) {
                    }
                    if (image != null) {
                        retries = 0;
                        image = Scalr.resize(image, Scalr.Mode.FIT_TO_HEIGHT, 1000 / count, 1000);
                        if (image != null) {
                            if (left + image.getWidth() <= width) {
                                nowImages.add(image);
                                filesDisplayed.add(filename);
                                System.out.println(filename);
                                left += image.getWidth();
                            }
                            i++;
                        }
                    }
                } else {
                    if (++retries > 200) {
                        filesDisplayed.clear();
                    }
                }
            }

            int x0 = (width - left) / (nowImages.size() + 1);
            int x = x0;
            for (BufferedImage nowImage : nowImages) {
                Image img = SwingFXUtils.toFXImage(nowImage, null);
                gc.drawImage(img, x, 0);
                x += nowImage.getWidth() + x0;
            }
        }

    }


    private String getRandomImage() {

        String filename;

        int source = minFolderIndex + (int)Math.floor((Math.random()*(1+maxFolderIndex-minFolderIndex)));

        Map<Integer, String> files = allImages.get(source);

        if ( files!=null && !files.isEmpty() ) {
            int file = (int)Math.floor(( minImageFactors.get(source)+(Math.random()*(maxImageFactors.get(source)-minImageFactors.get(source)))) * files.size());
            filename = files.get(file);
            if (filename != null && !filesDisplayed.contains(filename)) {
                return filename;
            }
        }
        return null;
    }



    private Set<String> filesDisplayed = new HashSet<>();

    int folderKey = 0;
    private void loadImages(File dir) {
        try {
            File[] files = dir.listFiles();
            int myFileKey = 0;
            int myFolderKey = -1;
            for (File file : files) {
                if (file.isDirectory() ) {
                    loadImages(file);
                } else {
                    if ( myFolderKey == -1 ) {
                        myFolderKey = folderKey++;
                        allImages.put(myFolderKey, new HashMap<>());
                        minImageFactors.put(myFolderKey, 0.0);
                        maxImageFactors.put(myFolderKey, 0.2);
                    }
                    allImages.get(myFolderKey).put(myFileKey++, file.getCanonicalPath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("--- SlideShow ---");
        if ( args.length>0 ) {
            startFolder = args[0];
            launch(args);
        }
    }

}
