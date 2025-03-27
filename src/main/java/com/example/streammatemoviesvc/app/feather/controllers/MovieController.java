package com.example.streammatemoviesvc.app.feather.controllers;

import com.example.streammatemoviesvc.app.feather.models.dtos.CinemaRecordResponse;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import com.example.streammatemoviesvc.app.feather.services.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class MovieController {

    private final MovieService movieService;

    @Autowired
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @DeleteMapping("/delete-movie-comment")
    public void deleteMovieComment(@RequestParam String commentId,
                                   @RequestParam String movieId) {

        this.movieService.deleteMovieComment(commentId, movieId);
    }

    @GetMapping("/get-next-10-movie-comments")
    public List<MovieComment> getNext10Comments(@RequestParam int order,
                                                @RequestParam String currentCinemaRecordId) {

        return this.movieService.getNext10Comments(order, UUID.fromString(currentCinemaRecordId));
    }

    @PostMapping("/post-movie-comment")
    public void postComment(@RequestParam String authorUsername,
                            @RequestParam String authorFullName,
                            @RequestParam String authorImgURL,
                            @RequestParam String commentText,
                            @RequestParam double rating,
                            @RequestParam String createdAt,
                            @RequestParam String authorId,
                            @RequestParam String movieId) {

        this.movieService.postComment(authorUsername, authorFullName, authorImgURL, commentText, rating, createdAt, authorId, movieId);
    }

    @GetMapping("/get-searched-movies-count")
    public long getSearchedMoviesCount(@RequestParam String title) {
        return this.movieService.getSearchedMoviesCount(title);
    }

    @GetMapping("/get-movies-by-title")
    public List<Movie> getMoviesByTitle(@RequestParam String title) {
        List<Movie> moviesByTitle = this.movieService.getMoviesByTitle(title);
        return moviesByTitle;
    }

    @GetMapping("/get-movies-count-by-genre")
    public long findMoviesCountByGenre(@RequestParam String genres) {
        return this.movieService.findMoviesCountByGenre(genres);
    }

    @GetMapping("/get-next-twenty-movies-by-genre")
    public List<CinemaRecordResponse> getNextTwentyMoviesByGenre(@RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 @RequestParam String receivedGenre) {

        Pageable pageable = PageRequest.of(page, size);  // Стандартен Pageable
        return movieService.getNextTwentyMoviesByGenre(receivedGenre, pageable);  // Предаваме жанра и Pageable на сървиса
    }


    @GetMapping("/get-movie-details")
    public Movie getConcreteMovieDetails(@RequestParam String id) {
        Movie movie = this.movieService.getConcreteMovieDetails(UUID.fromString(id));
        System.out.println();
        return movie;
    }

    @GetMapping("/get-next-thirty-movies")
    public List<CinemaRecordResponse> getEveryThirtyMovies(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CinemaRecordResponse> everyThirtyMovies = movieService.getEveryThirtyMovies(pageable);
        System.out.println("Requested page: " + page + ", size: " + size); // Дебъгване

        List<CinemaRecordResponse> movies = new ArrayList<>();
        everyThirtyMovies.get().forEach(movies::add);
        return movies;
    }

    @PostMapping("/search-movies")
    public void searchMovies(@RequestBody String title) throws IOException, InterruptedException {
        this.movieService.searchForMovies(title);
    }

    @GetMapping("/get-all-movies-count")
    public long getAllMoviesCount() {
        return this.movieService.getAllMoviesCount();
    }
}
