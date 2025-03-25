package com.example.streammatemoviesvc.app.feather.models.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movies_comments")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MovieComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, name = "comment_text")
    private String commentText;

    @Column(nullable = false, name = "author_username")
    private String authorUsername;

    @Column(nullable = false, name = "author_full_name")
    private String authorFullName;

    @Column(name = "author_img_url")
    private String authorImgURL;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false)
    @Min(1)
    private double rating;

    @Column(nullable = false, name = "created_at")
    private String createdAt;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    @JsonBackReference
    private Movie movie;
}
