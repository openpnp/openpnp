package org.openpnp.gui.components.nav;

import org.openpnp.events.JobLoadedEvent;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;

public class JobView extends Group {
    ObjectProperty<Job> jobProperty = new SimpleObjectProperty<>();
     
    public JobView() {
        Configuration.get().getBus().register(this);
    }
    
    @Subscribe
    public void jobLoaded(JobLoadedEvent e) {
        Platform.runLater(() -> {
            getChildren().clear();
            for (BoardLocation boardLocation : e.job.getBoardLocations()) {
                BoardLocationView boardLocationView = new BoardLocationView(boardLocation);
                getChildren().add(boardLocationView);
            }
            jobProperty.set(e.job);
        });
    }
    
    public ObjectProperty<Job> jobProperty() {
        return jobProperty;
    }
}
