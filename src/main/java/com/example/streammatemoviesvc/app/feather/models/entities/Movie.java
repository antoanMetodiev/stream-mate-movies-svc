package com.example.streammatemoviesvc.app.feather.models.entities;

import com.example.streammatemoviesvc.app.commonData.models.CinemaRecord;
import com.example.streammatemoviesvc.app.commonData.models.entities.Actor;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "movies")
@Getter
@Setter
@Accessors(chain = true)
public class Movie extends CinemaRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonManagedReference
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "movies_actors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    private List<Actor> castList = new ArrayList<>();

    @Column(name = "video_url", nullable = false)
    private String videoURL;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<MovieImage> imagesList = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<MovieComment> movieComments = new ArrayList<>();

    public void addAllImages(List<MovieImage> allImages) {
        allImages.forEach(image -> image.setMovie(this));
        this.getImagesList().addAll(allImages);
    }
}