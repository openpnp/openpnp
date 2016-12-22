package org.openpnp.gui.components.nav;

import java.util.HashMap;

import org.openpnp.events.JobLoadedEvent;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;

public class JobView extends Group {
    ObjectProperty<Job> jobProperty = new SimpleObjectProperty<>();
     
    public JobView() {
        Configuration.get().getBus().register(this);
    }
    
    @Subscribe
    public void jobLoaded(JobLoadedEvent ev) {
        ev.job.addPropertyChangeListener("boardLocations", e -> updateBoardLocations(ev.job));

        updateBoardLocations(ev.job);
    }
    
    private void updateBoardLocations(Job job) {
        Platform.runLater(() -> {
            HashMap<BoardLocation, BoardLocationView> views = new HashMap<>();
            for (Node node : getChildren()) {
                BoardLocationView view = (BoardLocationView) node;
                views.put(view.boardLocation, view);
            }
            // Add any missing children
            for (BoardLocation boardLocation : job.getBoardLocations()) {
                if (!views.containsKey(boardLocation)) {
                    BoardLocationView boardLocationView = new BoardLocationView(boardLocation);
                    getChildren().add(boardLocationView);
                }
            }
            // Remove each child that is no longer in the job
            for (BoardLocation boardLocation : views.keySet()) {
                if (!job.getBoardLocations().contains(boardLocation)) {
                    getChildren().remove(views.get(boardLocation));
                }
            }
            
            // This is a bit of a hack to let the nav panel know that the job has
            // been loaded and it should re-zoom.
            jobProperty.set(job);
        });
    }
    
    public ObjectProperty<Job> jobProperty() {
        return jobProperty;
    }
}
