package com.example.streammatemoviesvc;

import com.example.streammatemoviesvc.app.feather.controllers.MovieController;
import com.example.streammatemoviesvc.app.feather.models.dtos.CinemaRecordResponse;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import com.example.streammatemoviesvc.app.feather.services.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
public class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private MovieService movieService;  // Mockito mock

    @InjectMocks
    private MovieController movieController;  // Auto inject mocked service into controller

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(movieController).build();
    }

    @Test
    public void testGetAllMoviesCount() throws Exception {
        long expectedCount = 100L;

        // Настройка на мока
        when(movieService.getAllMoviesCount()).thenReturn(expectedCount);

        mockMvc.perform(get("/get-all-movies-count"))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedCount)));
    }

    @Test
    public void testGetEveryThirtyMovies() throws Exception {
        int page = 0;
        int size = 10;

        CinemaRecordResponse movie1 = new CinemaRecordResponse();
        movie1.setTitle("Movie 1");
        CinemaRecordResponse movie2 = new CinemaRecordResponse();
        movie2.setTitle("Movie 2");

        List<CinemaRecordResponse> mockMovies = Arrays.asList(movie1, movie2);

        Page<CinemaRecordResponse> mockPage = new PageImpl<>(mockMovies);
        when(movieService.getEveryThirtyMovies(PageRequest.of(page, size))).thenReturn(mockPage);

        mockMvc.perform(get("/get-next-thirty-movies")
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Movie 1"))
                .andExpect(jsonPath("$[1].title").value("Movie 2"));
    }

    @Test
    public void testGetConcreteMovieDetails() throws Exception {
        String movieId = "123e4567-e89b-12d3-a456-426614174000";
        Movie mockMovie = new Movie();
        mockMovie.setId(UUID.fromString(movieId));
        mockMovie.setTitle("Inception");
        when(movieService.getConcreteMovieDetails(UUID.fromString(movieId))).thenReturn(mockMovie);

        mockMvc.perform(get("/get-movie-details")
                        .param("id", movieId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    public void testGetNextTwentyMoviesByGenre() throws Exception {
        String genre = "Action";
        int page = 0;
        int size = 20;

        CinemaRecordResponse movie1 = new CinemaRecordResponse();
        movie1.setTitle("Action Movie 1");
        CinemaRecordResponse movie2 = new CinemaRecordResponse();
        movie2.setTitle("Action Movie 2");

        List<CinemaRecordResponse> mockMovies = Arrays.asList(movie1, movie2);
        when(movieService.getNextTwentyMoviesByGenre(genre, PageRequest.of(page, size))).thenReturn(mockMovies);
        mockMvc.perform(get("/get-next-twenty-movies-by-genre")
                        .param("receivedGenre", genre)
                        .param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Action Movie 1"))
                .andExpect(jsonPath("$[1].title").value("Action Movie 2"));
    }

    @Test
    public void testFindMoviesCountByGenre() throws Exception {
        String genre = "Action";
        long expectedCount = 10L;

        when(movieService.findMoviesCountByGenre(genre)).thenReturn(expectedCount);
        mockMvc.perform(get("/get-movies-count-by-genre")
                        .param("genres", genre))
                .andExpect(status().isOk())
                .andExpect(content().string(String.valueOf(expectedCount)));
    }

    @Test
    public void testGetMoviesByTitle() throws Exception {
        String title = "Inception";
        Movie movie1 = new Movie();
        movie1.setTitle("Inception");
        Movie movie2 = new Movie();
        movie2.setTitle("Inception");

        List<Movie> mockMovies = Arrays.asList(movie1, movie2);
        when(movieService.getMoviesByTitle(title)).thenReturn(mockMovies);

        mockMvc.perform(get("/get-movies-by-title")
                        .param("title", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[1].title").value("Inception"));
    }

    @Test
    public void testGetSearchedMoviesCount() throws Exception {
        String title = "Inception";
        long expectedCount = 5L;

        when(movieService.getSearchedMoviesCount(title)).thenReturn(expectedCount);
        mockMvc.perform(get("/get-searched-movies-count")
                        .param("title", title))
                .andExpect(status().isOk());
    }

    @Test
    public void testPostMovieComment() throws Exception {
        String authorUsername = "author123";
        String authorFullName = "John Doe";
        String authorImgURL = "http://example.com/image.jpg";
        String commentText = "Great movie!";
        double rating = 4.5;
        String createdAt = "2025-03-27T10:00:00";
        String authorId = "authorId123";
        String movieId = "movieId123";

        mockMvc.perform(post("/post-movie-comment")
                        .param("authorUsername", authorUsername)
                        .param("authorFullName", authorFullName)
                        .param("authorImgURL", authorImgURL)
                        .param("commentText", commentText)
                        .param("rating", String.valueOf(rating))
                        .param("createdAt", createdAt)
                        .param("authorId", authorId)
                        .param("movieId", movieId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(movieService, times(1)).postComment(authorUsername, authorFullName,
                authorImgURL, commentText, rating, createdAt, authorId, movieId);
    }

    @Test
    public void testGetNext10Comments() throws Exception {
        int order = 1;
        String currentCinemaRecordId = UUID.randomUUID().toString();
        MovieComment comment1 = new MovieComment();
        comment1.setCommentText("This is the first comment");
        MovieComment comment2 = new MovieComment();
        comment2.setCommentText("This is the second comment");

        List<MovieComment> mockComments = Arrays.asList(comment1, comment2);
        when(movieService.getNext10Comments(order, UUID.fromString(currentCinemaRecordId)))
                .thenReturn(mockComments);

        mockMvc.perform(get("/get-next-10-movie-comments")
                        .param("order", String.valueOf(order))
                        .param("currentCinemaRecordId", currentCinemaRecordId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testDeleteMovieComment() throws Exception {
        String commentId = "someCommentId";
        String movieId = "someMovieId";

        MvcResult result = mockMvc.perform(delete("/delete-movie-comment")
                        .param("commentId", commentId)
                        .param("movieId", movieId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        verify(movieService, times(1)).deleteMovieComment(commentId, movieId);
    }
}
