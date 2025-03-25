package com.example.streammatemoviesvc;

import com.example.streammatemoviesvc.app.commonData.models.entities.Actor;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

    @InjectMocks
    private MovieService movieService;

    @Test
    public void testUrlGeneration() {
        String movieId = "12345";
        String searchQuery = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=YOUR_API_KEY";

        assertEquals("https://api.themoviedb.org/3/movie/12345?api_key=YOUR_API_KEY", searchQuery);
    }

    @Test
    public void testSaveMovieWhenMovieDoesNotExist() {
        // Създаваме обектите
        String cinemaRecTitle = "Inception";
        String cinemaRecPosterImage = "inception.jpg";
        Movie movie = new Movie();
        movie.setTitle(cinemaRecTitle);
        movie.setPosterImgURL(cinemaRecPosterImage);

        // Създаваме актьори
        Actor actor1 = new Actor();
        actor1.setId(null);
        actor1.setNameInRealLife("Leonardo DiCaprio");

        Actor actor2 = new Actor();
        actor2.setId(null);
        actor2.setNameInRealLife("Joseph Gordon-Levitt");

        movie.setCastList(List.of(actor1, actor2));

        // Мокиране на резултати от репозиториите
        when(movieRepository.findByTitleAndPosterImgURL(cinemaRecTitle, cinemaRecPosterImage)).thenReturn(Optional.empty());
        when(actorRepository.save(any(Actor.class))).thenReturn(actor1, actor2); // Записваме новите актьори

        // Извикваме метода
        movieService.saveMovie(cinemaRecTitle, cinemaRecPosterImage, movie);

        // Проверяваме дали методът е бил извикан
        verify(movieRepository, times(1)).save(movie);  // Филмът трябва да бъде записан
        verify(actorRepository, times(2)).save(any(Actor.class));  // Всеки актьор трябва да бъде записан
    }

    @Test
    public void testSaveMovieWhenMovieExists() {
        // Създаваме обектите
        String cinemaRecTitle = "Inception";
        String cinemaRecPosterImage = "inception.jpg";
        Movie movie = new Movie();
        movie.setTitle(cinemaRecTitle);
        movie.setPosterImgURL(cinemaRecPosterImage);

        // Мокиране на резултати от репозиториите
        when(movieRepository.findByTitleAndPosterImgURL(cinemaRecTitle, cinemaRecPosterImage))
                .thenReturn(Optional.of(movie));

        // Извикваме метода
        movieService.saveMovie(cinemaRecTitle, cinemaRecPosterImage, movie);

        // Проверяваме дали методът е бил извикан
        verify(movieRepository, times(0)).save(movie);  // Филмът не трябва да бъде записан, защото вече съществува
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
    void testGetNext10Comments() {
        // Arrange
        int order = 2;
        UUID movieId = UUID.randomUUID();
        List<MovieComment> mockComments = List.of(new MovieComment(), new MovieComment());

        when(movieRepository.getNext10Comments(10, movieId)).thenReturn(mockComments);

        // Act
        List<MovieComment> result = movieService.getNext10Comments(order, movieId);

        // Assert
        assertEquals(mockComments.size(), result.size());
        verify(movieRepository, times(1)).getNext10Comments(10, movieId);
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