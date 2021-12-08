package com.example.mp3playerfx;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable {

    @FXML
    private Pane pane;
    @FXML
    private Label lblSong, lblArtist, lblCurrentTime, lblDuration, lblVolume;
    @FXML
    private Button btnPlay, btnChooseFile, btnPrevious, btnNext, btnStop;
    @FXML
    private ToggleButton toggleRepeat;
    @FXML
    private Slider sldVolume;
    @FXML
    private Slider sliderSongProgress;
    @FXML
    private TableView tableSongs;

    private Media media;
    private MediaPlayer mediaPlayer;

    private String filePath;

    private ArrayList<File> songs;
    private ArrayList<Mp3Metadata> metadatas;
    private ObservableList<Mp3Metadata> ol= FXCollections.observableArrayList();
    private final Object obj= new Object();

    private int songNumber;

    private boolean running;
    private boolean repeat;

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        songNumber = 0;
        songs = new ArrayList<File>();
        metadatas = new ArrayList<Mp3Metadata>();
        repeat = false;

        initSliders();
        initButtons();
        initTable();

    }

    public void chooseFiles() {

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("MP3 Files", "*.mp3");
        fileChooser.getExtensionFilters().add(filter);

        List<File> files = fileChooser.showOpenMultipleDialog(null);

        if(files != null) {

            try {

                for(File file : files) {

                    filePath = file.toURI().toString();
                    songs.add(file);

                    final MediaPlayer mp = new MediaPlayer(new Media( filePath ));
                    mp.setOnReady(new Runnable() {

                        @Override
                        public void run() {

                            String title = (String) mp.getMedia().getMetadata().get("title");
                            String artist = (String) mp.getMedia().getMetadata().get("artist");
                            String album = (String) mp.getMedia().getMetadata().get("album");

                            if(title == null) {
                                title = songs.get( songs.size() - 1 ).getName();
                            }

                            Mp3Metadata metadata = new Mp3Metadata(title, artist, album);
                            metadatas.add(metadata);
                            tableSongs.getItems().add(metadata);

                            ol.add(metadata);
                            synchronized (obj) {
                                obj.notify();
                            }

                        }

                    });

                    synchronized (obj) {
                        obj.wait(100);
                    }
                    System.gc();

                }

            } catch (InterruptedException ex) {

            }

        }

    }

    public void volumeUp() {

        sldVolume.setValue( sldVolume.getValue() + 5 );

    }

    public void volumeDown() {

        sldVolume.setValue( sldVolume.getValue() - 5 );

    }

    public void skipSeconds() {

        if(mediaPlayer != null) {

            mediaPlayer.seek( mediaPlayer.getCurrentTime().add( Duration.seconds(5) ) );
            sliderSongProgress.setValue( sliderSongProgress.getValue() + 5 );

            int minutes = (int) mediaPlayer.getCurrentTime().toMinutes();
            int seconds = (int) ( mediaPlayer.getCurrentTime().toSeconds() % 60 );
            String duration = String.format("%02d:%02d", minutes, seconds);

            lblCurrentTime.setText(duration);

        }

    }

    public void backSeconds() {

        if(mediaPlayer != null) {

            mediaPlayer.seek( mediaPlayer.getCurrentTime().add( Duration.seconds(-5) ) );
            sliderSongProgress.setValue( sliderSongProgress.getValue() - 5 );

            int minutes = (int) mediaPlayer.getCurrentTime().toMinutes();
            int seconds = (int) ( mediaPlayer.getCurrentTime().toSeconds() % 60 );
            String duration = String.format("%02d:%02d", minutes, seconds);

            lblCurrentTime.setText(duration);

        }

    }

    public void playOrPause() {

        if(mediaPlayer != null) {

            if(!running) {

                playMedia();
                setPauseIcon();

            } else {

                pauseMedia();
                setPlayIcon();

            }

        } else if(mediaPlayer == null && !songs.isEmpty()) {

            File song = songs.get(0);

            media = new Media( song.toURI().toString() );

            if(mediaPlayer != null) {
                mediaPlayer.stop();
            }
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnReady(new Runnable() {
                @Override
                public void run() {

                    sliderSongProgress.setMax( media.getDuration().toSeconds() );
                    sliderSongProgress.setValue(0);

                    int minutes = (int) media.getDuration().toMinutes();
                    int seconds = (int) ( media.getDuration().toSeconds() % 60 );
                    String duration = String.format("%02d:%02d", minutes, seconds);

                    lblDuration.setText(duration);

                    System.out.println(duration);

                }
            });
            mediaPlayer.setOnEndOfMedia(new Runnable() {
                @Override
                public void run() {

                    if(!repeat) {
                        nextMedia();
                    } else {
                        mediaPlayer.seek(Duration.seconds(0));
                        mediaPlayer.play();
                    }

                }
            });
            mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
                @Override
                public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {

                    sliderSongProgress.setValue(newValue.toSeconds());

                    int minutes = (int) newValue.toMinutes();
                    int seconds = (int) ( newValue.toSeconds() % 60 );
                    String duration = String.format("%02d:%02d", minutes, seconds);

                    lblCurrentTime.setText(duration);

                }
            });

            playOrPause();
            lblSong.setText( metadatas.get(0).getTitle() );
            lblArtist.setText( metadatas.get(0).getArtist() );
            tableSongs.getSelectionModel().select(0);

        } else if(songs.isEmpty()) {

            chooseFiles();

        }

    }

    public void playMedia() {

        mediaPlayer.setVolume( sldVolume.getValue() * 0.01 );
        mediaPlayer.play();
        running = true;

    }


    public void pauseMedia() {

        mediaPlayer.pause();
        running = false;

    }

    public void nextMedia() {

        if(mediaPlayer != null) {

            if(songNumber < songs.size() - 1) {

                songNumber++;
                updatePlayer();

            } else {

                songNumber = 0;
                updatePlayer();

            }

        }

    }

    public void previousMedia() {

        if(mediaPlayer != null) {

            if(songNumber > 0) {

                songNumber--;
                updatePlayer();

            } else {

                songNumber = songs.size() - 1;
                updatePlayer();

            }

        }

    }

    public void stopMedia() {

        if(mediaPlayer != null) {

            mediaPlayer.stop();
            mediaPlayer = null;
            running = false;
            songNumber = 0;

            lblSong.setText("MP3 Player FX");
            lblArtist.setText("");
            lblCurrentTime.setText("00:00");
            lblDuration.setText("00:00");

        }

    }

    public void deleteSong() {

        if(!tableSongs.getItems().isEmpty()) {

            int index = tableSongs.getSelectionModel().getSelectedIndex();

            if(index >= 0) {

                tableSongs.getItems().remove( index );
                metadatas.remove( index );
                songs.remove( index );

                if(mediaPlayer != null && running && songNumber == index) {

                    mediaPlayer.stop();
                    mediaPlayer = null;
                    running = false;
                    songNumber = 0;

                    lblSong.setText("MP3 Player FX");
                    lblArtist.setText("");
                    lblCurrentTime.setText("00:00");
                    lblDuration.setText("00:00");

                    Image iconImg = new Image(getClass().getResourceAsStream("img/play.png"),
                            55, 55, true, true);
                    btnPlay.setGraphic( new ImageView(iconImg) );

                }

                if(songNumber > index) {
                    songNumber--;
                }

                if(songNumber < 0 && !songs.isEmpty()) {
                    songNumber = 0;
                }

            }

        }

    }

    public void setRepeat() {

        repeat = !repeat;
        System.out.println(repeat);

    }

    public boolean isMediaPlayerSet() {

        if(mediaPlayer != null) {
            return true;
        } else {
            return false;
        }

    }

    private void updatePlayer() {

        mediaPlayer.stop();

        media = new Media( songs.get(songNumber).toURI().toString() );

        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setOnReady(new Runnable() {
            @Override
            public void run() {

                sliderSongProgress.setMax( media.getDuration().toSeconds() );
                sliderSongProgress.setValue(0);

                int minutes = (int) media.getDuration().toMinutes();
                int seconds = (int) ( media.getDuration().toSeconds() % 60 );
                String duration = String.format("%02d:%02d", minutes, seconds);
                System.out.println(duration);

                lblDuration.setText(duration);

            }
        });
        mediaPlayer.setOnEndOfMedia(new Runnable() {
            @Override
            public void run() {

                if(!repeat) {
                    nextMedia();
                } else {
                    mediaPlayer.seek(Duration.seconds(0));
                    mediaPlayer.play();
                }

            }
        });
        mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {

                sliderSongProgress.setValue(newValue.toSeconds());

                int minutes = (int) newValue.toMinutes();
                int seconds = (int) ( newValue.toSeconds() % 60 );
                String duration = String.format("%02d:%02d", minutes, seconds);

                lblCurrentTime.setText(duration);

            }
        });

        running = false;

        lblSong.setText( metadatas.get(songNumber).getTitle() );
        lblArtist.setText( metadatas.get(songNumber).getArtist() );
        tableSongs.getSelectionModel().select(songNumber);
        playOrPause();

    }



    // Methods to initialize

    private void initSliders() {

        sldVolume.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

                if(mediaPlayer != null) {

                    mediaPlayer.setVolume( sldVolume.getValue() * 0.01 );

                }

                double percentage = 100.0 * (newValue.doubleValue() - sldVolume.getMin()) /
                        (sldVolume.getMax() - sldVolume.getMin());

                StackPane sliderTrack = (StackPane) sldVolume.lookup(".track");
                sliderTrack.setStyle("-fx-background-color: " +
                        "linear-gradient(to right, #00cffa " + percentage + "%, transparent " + percentage + "%);");

                lblVolume.setText( (int) sldVolume.getValue() + "%" );

            }
        });
        sldVolume.setFocusTraversable(false);

        sliderSongProgress.setStyle("-fx-accent: #00FF00;");

        sliderSongProgress.valueProperty().addListener((obs, oldValue, newValue) -> {

            double percentage = 100.0 * (newValue.doubleValue() - sliderSongProgress.getMin()) /
                    (sliderSongProgress.getMax() - sliderSongProgress.getMin());

            StackPane sliderTrack = (StackPane) sliderSongProgress.lookup(".track");
            sliderTrack.setStyle("-fx-background-color: " +
                    "linear-gradient(to right, #00cffa " + percentage + "%, transparent " + percentage + "%);");

        });

        sliderSongProgress.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if(mediaPlayer != null) {

                    mediaPlayer.seek( Duration.seconds( sliderSongProgress.getValue() ) );

                    int minutes = (int) mediaPlayer.getCurrentTime().toMinutes();
                    int seconds = (int) ( mediaPlayer.getCurrentTime().toSeconds() % 60 );
                    String duration = String.format("%02d:%02d", minutes, seconds);

                    lblCurrentTime.setText(duration);

                    pane.requestFocus();

                }

            }
        });
        sliderSongProgress.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                if(mediaPlayer != null) {

                    mediaPlayer.seek( Duration.seconds( sliderSongProgress.getValue() ) );

                    int minutes = (int) mediaPlayer.getCurrentTime().toMinutes();
                    int seconds = (int) ( mediaPlayer.getCurrentTime().toSeconds() % 60 );
                    String duration = String.format("%02d:%02d", minutes, seconds);

                    lblCurrentTime.setText(duration);

                    pane.requestFocus();

                }

            }
        });
        sliderSongProgress.setFocusTraversable(false);

    }

    private void initButtons() {

        setPlayIcon();
        btnPlay.setStyle("-fx-background-color: transparent;");
        btnPlay.setFocusTraversable(false);

        Image iconImg = new Image(getClass().getResourceAsStream("img/next.png"),
                55, 55, true, true);
        btnNext.setGraphic( new ImageView(iconImg) );
        btnNext.setStyle("-fx-background-color: transparent;");
        btnNext.setFocusTraversable(false);

        iconImg = new Image(getClass().getResourceAsStream("img/previous.png"),
                55, 55, true, true);
        btnPrevious.setGraphic( new ImageView(iconImg) );
        btnPrevious.setStyle("-fx-background-color: transparent;");
        btnPrevious.setFocusTraversable(false);

        btnChooseFile.setFocusTraversable(false);
        btnStop.setFocusTraversable(false);
        toggleRepeat.setFocusTraversable(false);

    }

    private void initTable() {

        TableColumn<Mp3Metadata, String> titleColumn = new TableColumn<Mp3Metadata, String>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<Mp3Metadata, String>("title"));
        titleColumn.setSortable(false);

        TableColumn<Mp3Metadata, String> artistColumn = new TableColumn<Mp3Metadata, String>("Artist");
        artistColumn.setCellValueFactory(new PropertyValueFactory<Mp3Metadata, String>("artist"));
        artistColumn.setSortable(false);

        TableColumn<Mp3Metadata, String> albumColumn = new TableColumn<Mp3Metadata, String>("Album");
        albumColumn.setCellValueFactory(new PropertyValueFactory<Mp3Metadata, String>("album"));
        albumColumn.setSortable(false);

        tableSongs.getColumns().add(titleColumn);
        tableSongs.getColumns().add(artistColumn);
        tableSongs.getColumns().add(albumColumn);

        tableSongs.setRowFactory(tv -> {

            TableRow<Mp3Metadata> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");

            deleteItem.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {

                    tableSongs.getItems().remove( row.getIndex() );
                    metadatas.remove( row.getIndex() );
                    songs.remove( row.getIndex() );

                    if(mediaPlayer != null && running && songNumber == row.getIndex()) {

                        mediaPlayer.stop();
                        mediaPlayer = null;
                        running = false;
                        songNumber = 0;

                        lblSong.setText("MP3 Player FX");
                        lblArtist.setText("");
                        lblCurrentTime.setText("00:00");
                        lblDuration.setText("00:00");

                        Image iconImg = new Image(getClass().getResourceAsStream("img/play.png"),
                                55, 55, true, true);
                        btnPlay.setGraphic( new ImageView(iconImg) );

                    }

                    if(songNumber > row.getIndex()) {
                        songNumber--;
                    }

                    if(songNumber < 0 && !songs.isEmpty()) {
                        songNumber = 0;
                    }

                    System.out.println(songNumber);

                }

            });

            rowMenu.getItems().add(deleteItem);
            row.contextMenuProperty().bind(
                    Bindings.when( row.emptyProperty() )
                            .then((ContextMenu) null)
                            .otherwise(rowMenu)
            );

            row.setOnDragDetected(event -> {

                if (!row.isEmpty()) {

                    Integer index = row.getIndex();
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();

                    db.setDragView( row.snapshot(null, null) );
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);

                    event.consume();

                }

            });

            row.setOnDragOver(event -> {

                Dragboard db = event.getDragboard();

                if (db.hasContent(SERIALIZED_MIME_TYPE)) {

                    if (row.getIndex() != ((Integer) db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {

                        event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                        event.consume();

                    }

                }

            });

            row.setOnDragDropped(event -> {

                Dragboard db = event.getDragboard();

                if (db.hasContent(SERIALIZED_MIME_TYPE)) {

                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    int dropIndex = 0;
                    Mp3Metadata draggedMetadata = (Mp3Metadata) tableSongs.getItems().get(draggedIndex);

                    if (row.isEmpty()) {

                        dropIndex = tableSongs.getItems().size();

                    } else {

                        dropIndex = row.getIndex();

                    }

                    // Swaping data on Songs and Metadatas
                    tableSongs.getItems().set(draggedIndex, row.getItem());
                    tableSongs.getItems().set(dropIndex, draggedMetadata);
                    tableSongs.getSelectionModel().select(dropIndex);

                    metadatas.set(draggedIndex, row.getItem());
                    metadatas.set(dropIndex, draggedMetadata);

                    File draggedSong = songs.get(draggedIndex);
                    songs.set( draggedIndex, songs.get(row.getIndex()) );
                    songs.set(dropIndex, draggedSong);

                    if(songNumber == draggedIndex) {
                        songNumber = dropIndex;
                    }

                    System.out.println("Metadata: " + metadatas);

                    event.setDropCompleted(true);
                    event.consume();

                }

            });

            row.setOnMousePressed(new EventHandler<MouseEvent>() {

                @Override
                public void handle(MouseEvent event) {

                    if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {

                        Mp3Metadata metadata = (Mp3Metadata) tableSongs.getSelectionModel().selectedItemProperty().get();
                        int index = tableSongs.getSelectionModel().selectedIndexProperty().get();

                        if(!songs.isEmpty() && index != -1) {

                            File song = songs.get(index);

                            media = new Media( song.toURI().toString() );

                            if(mediaPlayer != null) {
                                mediaPlayer.stop();
                                running = false;
                            }
                            mediaPlayer = new MediaPlayer(media);
                            mediaPlayer.setOnReady(new Runnable() {
                                @Override
                                public void run() {

                                    sliderSongProgress.setMax( media.getDuration().toSeconds() );
                                    sliderSongProgress.setValue(0);

                                    int minutes = (int) media.getDuration().toMinutes();
                                    int seconds = (int) ( media.getDuration().toSeconds() % 60 );
                                    String duration = String.format("%02d:%02d", minutes, seconds);

                                    lblDuration.setText(duration);

                                    System.out.println(duration);

                                }
                            });
                            mediaPlayer.setOnEndOfMedia(new Runnable() {
                                @Override
                                public void run() {

                                    if(!repeat) {
                                        nextMedia();
                                    } else {
                                        mediaPlayer.seek(Duration.seconds(0));
                                        mediaPlayer.play();
                                    }

                                }
                            });
                            mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
                                @Override
                                public void changed(ObservableValue<? extends Duration> observable, Duration oldValue, Duration newValue) {

                                    sliderSongProgress.setValue(newValue.toSeconds());

                                    int minutes = (int) newValue.toMinutes();
                                    int seconds = (int) ( newValue.toSeconds() % 60 );
                                    String duration = String.format("%02d:%02d", minutes, seconds);

                                    lblCurrentTime.setText(duration);

                                }
                            });

                            songNumber = index;

                            lblSong.setText( metadata.getTitle() );
                            lblArtist.setText( metadata.getArtist() );
                            playOrPause();

                            System.out.println(metadata.getTitle());
                            System.out.println(index);

                        }

                    } else if( event.isPrimaryButtonDown() ) {

                        System.out.println( tableSongs.getSelectionModel().getFocusedIndex() );

                        @SuppressWarnings("unchecked")
                        TableRow<Mp3Metadata> row = (TableRow<Mp3Metadata>) event.getSource();

                        if(row.isEmpty() || row.getItem() == null) {

                            tableSongs.getSelectionModel().clearSelection();

                        }

                    }

                }

            });

            return row;

        });

        tableSongs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableSongs.setPlaceholder(new Label(""));
        tableSongs.setFocusTraversable(false);

    }

    private void setPlayIcon() {

        Image iconImg = new Image(getClass().getResourceAsStream("img/play.png"),
                55, 55, true, true);
        btnPlay.setGraphic( new ImageView(iconImg) );

    }

    private void setPauseIcon() {

        Image iconImg = new Image(getClass().getResourceAsStream("img/pause.png"),
                55, 55, true, true);
        btnPlay.setGraphic( new ImageView(iconImg) );

    }

}