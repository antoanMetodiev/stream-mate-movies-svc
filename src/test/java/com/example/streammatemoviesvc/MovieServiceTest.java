package com.example.streammatemoviesvc;

import com.example.streammatemoviesvc.app.commonData.models.enums.ImageType;
import com.example.streammatemoviesvc.app.commonData.repositories.ActorRepository;
import com.example.streammatemoviesvc.app.feather.models.dtos.CinemaRecordResponse;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieImage;
import com.example.streammatemoviesvc.app.feather.repositories.MovieCommentRepository;
import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import com.example.streammatemoviesvc.app.feather.services.MovieService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieCommentRepository movieCommentRepository;

    @Mock
    private ActorRepository actorRepository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private MovieService movieService;

    @Test
    void testPostComment() {
        // Arrange
        String authorUsername = "testUser";
        String authorFullName = "Test User";
        String authorImgURL = "http://example.com/image.jpg";
        String commentText = "Great movie!";
        double rating = 4.5;
        String createdAt = "2025-03-25";
        String authorId = UUID.randomUUID().toString();
        String movieId = UUID.randomUUID().toString();

        UUID movieUUID = UUID.fromString(movieId);
        Movie movie = new Movie();
        movie.setMovieComments(new ArrayList<>());

        when(movieRepository.findById(movieUUID)).thenReturn(Optional.of(movie));

        // Act
        movieService.postComment(authorUsername, authorFullName, authorImgURL, commentText, rating, createdAt, authorId, movieId);

        // Assert
        assertEquals(1, movie.getMovieComments().size());
        verify(movieRepository, times(1)).findById(movieUUID);
        verify(movieRepository, times(1)).save(movie);
    }

    @Test
    public void testUrlGeneration() {
        String movieId = "12345";
        String searchQuery = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=YOUR_API_KEY";

        assertEquals("https://api.themoviedb.org/3/movie/12345?api_key=YOUR_API_KEY", searchQuery);
    }

    @Test
    void testExtractDetailsImages() {
        // Arrange
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < 3; i++) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("file_path", "http://example.com/image" + i + ".jpg");
            jsonArray.add(jsonObject);
        }

        int limit = 2;

        // Act
        CompletableFuture<List<MovieImage>> futureResult = movieService.extractDetailsImages(jsonArray, ImageType.BACKDROP, limit);
        List<MovieImage> result = futureResult.join();

        // Assert
        assertNotNull(result);
        assertEquals("http://example.com/image0.jpg", result.get(0).getImageURL());
        assertEquals("http://example.com/image1.jpg", result.get(1).getImageURL());
        assertEquals(ImageType.BACKDROP, result.get(0).getImageType()); // Проверка дали типът на изображението е правилен
        assertEquals(ImageType.BACKDROP, result.get(1).getImageType());
    }

    @Test
    void testGetNext10Comments() {
        // Arrange
        int order = 2;
        UUID movieId = UUID.randomUUID();
        int offset = (order - 1) * 10;

        // Създаваме mock лист с Object[], където всеки Object[] представя MovieComment
        List<Object[]> mockComments = List.of(
                new Object[]{UUID.randomUUID(), "Great movie!", "user123", "John Doe", "img_url", UUID.randomUUID(), 4.5, "2024-03-29 12:00:00"},
                new Object[]{UUID.randomUUID(), "Not bad!", "user456", "Jane Doe", "img_url2", UUID.randomUUID(), 3.8, "2024-03-28 14:30:00"}
        );

        when(movieRepository.getNext10Comments(offset, movieId)).thenReturn(mockComments);

        // Act
        List<MovieComment> result = movieService.getNext10Comments(order, movieId);

        // Assert
        assertEquals(mockComments.size(), result.size());
        assertEquals("Great movie!", result.get(0).getCommentText());
        assertEquals("Not bad!", result.get(1).getCommentText());

        verify(movieRepository, times(1)).getNext10Comments(offset, movieId);
    }


    @Test
    void testDeleteMovieComment() {
        // Arrange
        UUID movieId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        Movie movie = new Movie();
        MovieComment comment = new MovieComment();
        comment.setId(commentId);
        movie.setMovieComments(new ArrayList<>(List.of(comment)));

        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));

        // Act
        movieService.deleteMovieComment(commentId.toString(), movieId.toString());

        // Assert
        assertTrue(movie.getMovieComments().isEmpty());
        verify(movieCommentRepository, times(1)).delete(comment);
        verify(movieRepository, times(1)).save(movie);
    }

    @Test
    void testGetNextTwentyMoviesByGenre() {
        // Arrange
        String genre = "Action";
        UUID movieId = UUID.randomUUID();
        String title = "Mad Max";
        String posterUrl = "http://example.com/madmax.jpg";
        String releaseDate = "2015-05-15";
        List<Object[]> mockRawData = new ArrayList<>();
        mockRawData.add(new Object[]{movieId, title, posterUrl, releaseDate});

        Pageable pageable = PageRequest.of(0, 20);
        when(movieRepository.findByGenreNextTwentyMovies(genre, 20, 0)).thenReturn(mockRawData);

        // Act
        List<CinemaRecordResponse> result = movieService.getNextTwentyMoviesByGenre(genre, pageable);

        // Assert
        assertEquals(1, result.size());
        assertEquals(title, result.get(0).getTitle());
        assertEquals(posterUrl, result.get(0).getPosterImgURL());
        assertEquals(releaseDate, result.get(0).getReleaseDate());
        verify(movieRepository, times(1)).findByGenreNextTwentyMovies(genre, 20, 0);
    }

    @Test
    void testGetSearchedMoviesCount() {
        // Arrange
        String title = "Inception";
        long expectedCount = 10L;
        when(movieRepository.findMoviesCountByTitleOrSearchTagContainingIgnoreCase(title)).thenReturn(expectedCount);

        // Act
        long result = movieService.getSearchedMoviesCount(title);

        // Assert
        assertEquals(expectedCount, result);
        verify(movieRepository, times(1)).findMoviesCountByTitleOrSearchTagContainingIgnoreCase(title);
    }

    @Test
    void testGetMoviesByTitle() {
        // Arrange
        String title = "Inception";
        List<Movie> mockMovies = List.of(new Movie(), new Movie());
        when(movieRepository.findByTitleOrSearchTagContainingIgnoreCase(title)).thenReturn(mockMovies);

        // Act
        List<Movie> result = movieService.getMoviesByTitle(title);

        // Assert
        assertEquals(mockMovies.size(), result.size());
        verify(movieRepository, times(1)).findByTitleOrSearchTagContainingIgnoreCase(title);
    }

    @Test
    void testFindMoviesCountByGenre() {
        // Arrange
        String genre = "Action";
        long expectedCount = 50L;
        when(movieRepository.findMoviesCountByGenre(genre)).thenReturn(expectedCount);

        // Act
        long result = movieService.findMoviesCountByGenre(genre);

        // Assert
        assertEquals(expectedCount, result);
        verify(movieRepository, times(1)).findMoviesCountByGenre(genre);
    }

    @Test
    void testGetAllMoviesCount() {
        // Arrange
        long expectedCount = 100L;
        when(movieRepository.count()).thenReturn(expectedCount);

        // Act
        long result = movieService.getAllMoviesCount();

        // Assert
        assertEquals(expectedCount, result);
        verify(movieRepository, times(1)).count();
    }

    @Test
    void testGetConcreteMovieDetails_Found() {
        // Arrange
        UUID movieId = UUID.randomUUID();
        Movie mockMovie = new Movie();
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(mockMovie));

        // Act
        Movie result = movieService.getConcreteMovieDetails(movieId);

        // Assert
        assertEquals(mockMovie, result);
        verify(movieRepository, times(1)).findById(movieId);
    }

    @Test
    void testGetConcreteMovieDetails_NotFound() {
        // Arrange
        UUID movieId = UUID.randomUUID();
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> movieService.getConcreteMovieDetails(movieId));
        verify(movieRepository, times(1)).findById(movieId);
    }

    @Test
    void testGetEveryThirtyMovies() {
        // Arrange
        UUID movieId = UUID.randomUUID();
        String title = "Inception";
        String posterUrl = "http://example.com/poster.jpg";
        String releaseDate = "2010-07-16";

        List<Object[]> mockRawData = new ArrayList<>();
        mockRawData.add(new Object[]{movieId, title, posterUrl, releaseDate});

        Pageable pageable = PageRequest.of(0, 30);

        when(movieRepository.getThirthyMoviesRawData(30, 0)).thenReturn(mockRawData);

        // Act
        Page<CinemaRecordResponse> result = movieService.getEveryThirtyMovies(pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(title, result.getContent().get(0).getTitle());
        assertEquals(posterUrl, result.getContent().get(0).getPosterImgURL());
        assertEquals(releaseDate, result.getContent().get(0).getReleaseDate());

        verify(movieRepository, times(1)).getThirthyMoviesRawData(30, 0);
    }

    @Test
    public void testExtractDetailsImagesWithNullParams() {
        // Когато входните параметри са null, очакваме да се върне празен списък
        List<MovieImage> images = movieService.extractDetailsImages(null, ImageType.BACKDROP, 1).join();
        assertTrue(images.isEmpty());

        images = movieService.extractDetailsImages(new JsonArray(), null, 1).join();
        assertTrue(images.isEmpty());
    }
}