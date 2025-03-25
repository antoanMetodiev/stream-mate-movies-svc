package com.example.streammatemoviesvc.app.feather.repositories;

import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovieCommentRepository extends JpaRepository<MovieComment, UUID> {
}
