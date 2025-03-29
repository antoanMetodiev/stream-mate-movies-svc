package com.example.streammatemoviesvc.app.feather.repositories;

import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    Optional<Movie> findByTitleAndPosterImgURL(String cinemaRecTitle, String cinemaRecPosterImage);

    @Query(value = "SELECT count(*) FROM movies WHERE LOWER(title) LIKE LOWER(CONCAT('%', :movieName, '%'))" +
            " OR LOWER(search_tag) LIKE LOWER(CONCAT('%', :movieName, '%'))", nativeQuery = true)
    long findMoviesCountByTitleOrSearchTagContainingIgnoreCase(@Param("movieName") String movieName);

    @Query(value = "SELECT * FROM movies WHERE LOWER(title) LIKE LOWER(CONCAT('%', :movieName, '%'))" +
            " OR LOWER(search_tag) LIKE LOWER(CONCAT('%', :movieName, '%'))", nativeQuery = true)
    List<Movie> findByTitleOrSearchTagContainingIgnoreCase(@Param("movieName") String movieName);

    @Query(value = "SELECT id, title, poster_img_url, release_date FROM movies ORDER BY created_at DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> getThirthyMoviesRawData(@Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT id, title, poster_img_url, release_date FROM movies WHERE LOWER(genres) LIKE LOWER(CONCAT('%', :genre, '%')) ORDER BY created_at DESC LIMIT :size OFFSET :offset", nativeQuery = true)
    List<Object[]> findByGenreNextTwentyMovies(@Param("genre") String genre, @Param("size") int size, @Param("offset") int offset);

    @Query(value = "SELECT COUNT(*) FROM movies WHERE LOWER(genres) LIKE LOWER(CONCAT('%', :genre, '%'))", nativeQuery = true)
    long findMoviesCountByGenre(@Param("genre") String genre);

    @Query(value =
            "SELECT id, comment_text, author_username, author_full_name, author_img_url, " +
                    "author_id, rating, created_at " +
                    "FROM movies_comments " +
                    "WHERE movie_id = :currentCinemaRecordId " +
                    "ORDER BY created_at DESC " +
                    "LIMIT 10 OFFSET :offset",
            nativeQuery = true)
    List<Object[]> getNext10Comments(@Param("offset") int offset,
                                     @Param("currentCinemaRecordId") UUID currentCinemaRecordId);
}