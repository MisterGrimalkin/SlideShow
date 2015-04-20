package slideshow;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
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

    private static String startFolder;

    private static boolean splitIntoBlocks = false;

    private long showImageFor = 8000;
    private int blockSize = 12;
    private int imagesToShowPerBlock = 4;

    private int width = 1700;
    private int height = 2000;

    private int currentBlock = 0;
    private int imagesShownForCurrentBlock = 0;
    private int totalFiles = 0;

    private Map<Integer, List<ImageInfo>> imageBlocks = new HashMap<>();
    private Set<String> filesDisplayed = new HashSet<>();

    private GraphicsContext gc;
    private Group board;


    @Override
    public void start(Stage stage) throws Exception{

        width = (int)Math.floor(Screen.getPrimary().getVisualBounds().getWidth());
        height = (int)Math.floor(Screen.getPrimary().getVisualBounds().getHeight()) + 50;

        loadImages(new File(startFolder));
        System.out.println("Loaded " + totalFiles + " files, in " + (currentBlock+1) + " blocks.");
        currentBlock = 0;

        final Pane pane = new Pane();
        board = new Group();
        pane.getChildren().add(board);

        stage.setScene(new Scene(pane, width, height));
        stage.setFullScreen(true);
        stage.setTitle("Slideshow");
        stage.show();

        Canvas canvas = new Canvas(width,height);
        gc = canvas.getGraphicsContext2D();
        gc.fillRect(10, 10, 10, 10);

        board.getChildren().add(canvas);

        new Timer(true).schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        putRandomImages(2);
                    }
                }, 0, showImageFor
        );

        pane.setOnMouseClicked(event -> {
            if ( event.isControlDown() ) {
                paused = !paused;
                System.out.println(paused ? "Pause" : "Resume");
            }
        });
    }

    private boolean paused = false;



    private void putRandomImages(int count) {
        if ( !paused ) {

            int left = 0;
            gc.fillRect(0, 0, width, height);
            int i = 0;
            int retries = 0;
            List<BufferedImage> nowImages = new ArrayList<>();
            while (i < count) {

                if ( imagesShownForCurrentBlock > imagesToShowPerBlock ) {
                    currentBlock++;
                    imagesShownForCurrentBlock = 0;
                }
                String filename = getRandomImage();

                if (filename != null && !filesDisplayed.contains(filename)) {
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
                                imagesShownForCurrentBlock++;
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

        if ( !splitIntoBlocks || currentBlock<imageBlocks.size() ) {
            List<ImageInfo> infos = imageBlocks.get(currentBlock);
            if ( infos!=null && !infos.isEmpty() ) {
                int index = (int)Math.floor(Math.random()*(infos.size()));
                ImageInfo info = infos.get(index);
                return info.filename;
            } else {
                currentBlock++;
                imagesShownForCurrentBlock = 0;
            }

        } else {
            currentBlock = 0;
            imagesShownForCurrentBlock = 0;
        }
        return null;
    }

    private void loadImages(File dir) {
        try {
            File[] files = dir.listFiles();
            if ( files!=null ) {
                Arrays.sort(files);
                int fileCount = 0;
                for (File file : files) {
                    if (file.isDirectory() ) {
                        if ( splitIntoBlocks ) { currentBlock++; }
                        loadImages(file);
                    } else {
                        if ( file.getName().length()>=4 ) {
                            String ext = file.getName().substring(file.getName().length() - 3);
                            if (      ext.equalsIgnoreCase("jpg")
                                    | ext.equalsIgnoreCase("peg")
                                    | ext.equalsIgnoreCase("png")
                                    | ext.equalsIgnoreCase("gif")
                                    | ext.equalsIgnoreCase("bmp")
                            ) {
                                if (!splitIntoBlocks || fileCount < blockSize) {
                                    List<ImageInfo> infos = imageBlocks.get(currentBlock);
                                    if (infos == null) {
                                        infos = new ArrayList<>();
                                        imageBlocks.put(currentBlock, infos);
                                    }
                                    infos.add(new ImageInfo(file.getCanonicalPath()));
//                                    System.out.println(file.getName());
                                    totalFiles++;
                                    fileCount++;
                                } else {
                                    fileCount = 0;
                                    currentBlock++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if ( args.length>0 ) {
            startFolder = args[0];
            if ( args.length>1 ) {
                splitIntoBlocks = "seq".equalsIgnoreCase(args[1]);
            }
            System.out.println("Starting SlideShow: " + startFolder);
            launch(args);
        }
    }

    private static class ImageInfo {
        String filename;
        int width = 0;
        int height = 0;
        public ImageInfo(String filename) {
            this.filename = filename;
        }
        public ImageInfo(String filename, int width, int height) {
            this.filename = filename;
            this.width = width;
            this.height = height;
        }
    }

}
