package com.example.streammatemoviesvc.app.feather.services;

import com.example.streammatemoviesvc.app.commonData.models.entities.Actor;
import com.example.streammatemoviesvc.app.commonData.models.enums.ImageType;
import com.example.streammatemoviesvc.app.commonData.repositories.ActorRepository;
import com.example.streammatemoviesvc.app.commonData.utils.UtilMethods;
import com.example.streammatemoviesvc.app.feather.models.dtos.CinemaRecordResponse;
import com.example.streammatemoviesvc.app.feather.models.entities.Movie;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieComment;
import com.example.streammatemoviesvc.app.feather.models.entities.MovieImage;
import com.example.streammatemoviesvc.app.feather.repositories.MovieCommentRepository;
import com.example.streammatemoviesvc.app.feather.repositories.MovieRepository;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class MovieService {
    private final String TMDB_API_KEY = System.getenv("TMDB_API_KEY");
    private final String TMDB_BASE_URL = System.getenv("TMDB_BASE_URL");

    private final HttpClient httpClient;
    private final ActorRepository actorRepository;
    private final MovieRepository movieRepository;
    private final MovieCommentRepository movieCommentRepository;

    private final TransactionTemplate transactionTemplate;
    private final Executor asyncExecutor;

    @Autowired
    public MovieService(HttpClient httpClient,
                        ActorRepository actorRepository,
                        MovieRepository movieRepository,
                        MovieCommentRepository movieCommentRepository,
                        TransactionTemplate transactionTemplate,
                        Executor asyncExecutor) {

        this.httpClient = httpClient;
        this.actorRepository = actorRepository;
        this.movieRepository = movieRepository;
        this.movieCommentRepository = movieCommentRepository;
        this.transactionTemplate = transactionTemplate;
        this.asyncExecutor = asyncExecutor;
    }


    public Page<CinemaRecordResponse> getEveryThirtyMovies(Pageable pageable) {
        int size = pageable.getPageSize();
        int offset = pageable.getPageNumber() * size;
        List<Object[]> rawData = movieRepository.getThirthyMoviesRawData(size, offset);

        List<CinemaRecordResponse> dtos = rawData.stream().map(obj ->
                new CinemaRecordResponse(
                        (UUID) obj[0],
                        (String) obj[1],  // title
                        (String) obj[2],  // posterImgURL
                        (String) obj[3]   // releaseDate
                )
        ).toList();

        return new PageImpl<>(dtos, pageable, dtos.size());
    }

    public long getAllMoviesCount() {
        return this.movieRepository.count();
    }

    public Movie getConcreteMovieDetails(UUID id) {
        return this.movieRepository.findById(id).orElseThrow();
    }

    public List<Movie> getMoviesByTitle(String title) {
        return this.movieRepository.findByTitleOrSearchTagContainingIgnoreCase(title);
    }

    public long findMoviesCountByGenre(String genre) {
        return this.movieRepository.findMoviesCountByGenre(genre);
    }

    public List<CinemaRecordResponse> getNextTwentyMoviesByGenre(String genre, Pageable pageable) {
        int size = pageable.getPageSize();  // Получаваме размера на страницата (напр. 20)
        int offset = pageable.getPageNumber() * size;  // Пресмятаме OFFSET (page * size)

        List<Object[]> moviesByGenres = movieRepository.findByGenreNextTwentyMovies(genre, size, offset);
        List<CinemaRecordResponse> dtos = moviesByGenres.stream().map(obj ->
                new CinemaRecordResponse(
                        (UUID) obj[0],
                        (String) obj[1],  // title
                        (String) obj[2],  // posterImgURL
                        (String) obj[3]   // releaseDate
                )
        ).toList();

        return dtos;
    }

    public long getSearchedMoviesCount(String title) {
        return this.movieRepository.findMoviesCountByTitleOrSearchTagContainingIgnoreCase(title);
    }

    @Transactional
    public void postComment(String authorUsername, String authorFullName,
                            String authorImgURL, String commentText, double rating,
                            String createdAt,
                            String authorId,
                            String movieId) {

        UUID id = UUID.fromString(movieId);
        Movie movie = this.movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie is not found!"));

        MovieComment comment = new MovieComment();
        comment.setAuthorUsername(authorUsername);
        comment.setAuthorFullName(authorFullName);
        comment.setAuthorImgURL(authorImgURL);
        comment.setCommentText(commentText);
        comment.setRating(rating);
        comment.setCreatedAt(createdAt);
        comment.setMovie(movie);
        comment.setAuthorId(UUID.fromString(authorId));

        movie.getMovieComments().add(comment);
        this.movieRepository.save(movie);
    }

    public List<MovieComment> getNext10Comments(int order, UUID currentCinemaRecordId) {
        int offset = (order - 1) * 10;  // Преобразуване на order в offset
        List<MovieComment> next10Comments = this.movieRepository.getNext10Comments(offset, currentCinemaRecordId);
        return next10Comments;
    }

    @Transactional
    public void deleteMovieComment(String commentId, String movieId) {
        UUID currentMovieId = UUID.fromString(movieId);
        UUID currentCommentId = UUID.fromString(commentId);

        // Изтегляне на филма по ID
        Movie movie = this.movieRepository.findById(currentMovieId)
                .orElseThrow(() -> new RuntimeException("Movie not found!"));

        MovieComment commentToDelete = movie.getMovieComments().stream()
                .filter(comment -> comment.getId().equals(currentCommentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Comment not found!"));

        movie.getMovieComments().remove(commentToDelete);
        this.movieCommentRepository.delete(commentToDelete);

        this.movieRepository.save(movie);
    }

    @Async
    public CompletableFuture<Void> searchForMovies(String movieName) {
        if (movieName.trim().isEmpty()) return new CompletableFuture<Void>();

        CompletableFuture.supplyAsync(() -> {
            try {
                String encodedMovieName = URLEncoder.encode(movieName, "UTF-8");
                String searchQuery = TMDB_BASE_URL + "/3/search/movie?api_key=" + TMDB_API_KEY + "&query=" + encodedMovieName;

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchQuery)).build();
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);
                    JsonArray results = jsonObject.get("results").getAsJsonArray();

                    for (JsonElement currentMovie : results) {

                        jsonObject = currentMovie.getAsJsonObject();
                        Movie movie = new Movie();

                        String movieId = UtilMethods.getJsonValue(jsonObject, "id");
                        String title = UtilMethods.getJsonValue(jsonObject, "title");
                        String description = UtilMethods.getJsonValue(jsonObject, "overview");
                        String releaseDate = UtilMethods.getJsonValue(jsonObject, "release_date");
                        String backgroundIMG = UtilMethods.getJsonValue(jsonObject, "backdrop_path");
                        String posterIMG = UtilMethods.getJsonValue(jsonObject, "poster_path");
                        String movieRating = UtilMethods.getJsonValue(jsonObject, "vote_average");

                        // Checks:
                        if (posterIMG.trim().isEmpty()) continue;
                        if (releaseDate.trim().isEmpty()) continue;
                        if (LocalDate.parse(releaseDate).isAfter(LocalDate.now())) continue;
                        if (LocalDate.parse(releaseDate).getYear() < 2000) continue;
                        if (movieRating.equals("0.0")) continue;

                        String VidURL = "https://vidsrc.net/embed/movie/" + movieId;
                        String castURL = TMDB_BASE_URL + "/3/movie/" + movieId + "/credits" + "?api_key=" + TMDB_API_KEY;

                        UtilMethods utilMethods = new UtilMethods();

                        // Стартираме асинхронни операции:
                        CompletableFuture<List<Actor>> asyncActors = utilMethods.extractActors(
                                castURL, this.httpClient, TMDB_BASE_URL, TMDB_API_KEY, asyncExecutor);
                        CompletableFuture<Boolean> extractedImages = extractImagesAsync(movieId, movie);
                        CompletableFuture<Boolean> extractGenresAndTaglineAsync = extractGenresAndTaglineAsync(movieId, encodedMovieName, movie);

                        // Изчакваме резултатите
                        List<Actor> actors = asyncActors.get();
                        addAllCast(actors, movie);
                        if (actors.isEmpty()) continue;
                        if (!extractedImages.get()) continue;
                        if (!extractGenresAndTaglineAsync.get()) continue;

                        // Запазвам крайният обект:
                        movie.setVideoURL(VidURL).setSearchTag(movieName).setTitle(title).setDescription(description)
                                .setReleaseDate(releaseDate).setBackgroundImg_URL(backgroundIMG)
                                .setPosterImgURL(posterIMG).setTmdbRating(movieRating)
                                .setCreatedAt(Instant.now());

                        saveMovie(title, posterIMG, movie);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }

            return null;
        }, asyncExecutor);

        return new CompletableFuture<Void>();
    }

    @Async
    public CompletableFuture<Boolean> extractGenresAndTaglineAsync(String movieId, String encodedMovieName, Movie movie) {
        String searchQuery = TMDB_BASE_URL + "/3/movie/" + movieId + "?api_key=" + TMDB_API_KEY;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchQuery)).build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);

                    String specialText = UtilMethods.getJsonValue(jsonObject, "tagline");
                    JsonElement genres = jsonObject.get("genres");
                    StringBuilder genresString = new StringBuilder();
                    genres.getAsJsonArray().forEach(genre -> {
                        genresString.append(UtilMethods.getJsonValue(genre.getAsJsonObject(), "name")).append(",");
                    });

                    if (genresString.isEmpty()) return false;
                    movie.setSpecialText(specialText).setGenres(genresString.toString());
                }

            } catch (Exception exception) {
                System.out.println(exception.getMessage());
            }

            return true;
        }, asyncExecutor);
    }

    @Async
    public CompletableFuture<Boolean> extractImagesAsync(String movieId, Movie movie) {
        String searchQuery = TMDB_BASE_URL + "/3/movie/" + movieId + "/images?api_key=" + TMDB_API_KEY;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(searchQuery)).build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject jsonObject = new Gson().fromJson(response.body(), JsonObject.class);
                    JsonArray backdropsJsonAr = jsonObject.getAsJsonArray("backdrops");
                    JsonArray postersJsonAr = jsonObject.getAsJsonArray("posters");

                    CompletableFuture<List<MovieImage>> allBackdropImgsFuture = extractDetailsImages(backdropsJsonAr, ImageType.BACKDROP, 29);
                    CompletableFuture<List<MovieImage>> allPosterImages = extractDetailsImages(postersJsonAr, ImageType.POSTER, 8);

                    List<MovieImage> allImages = new ArrayList<>();
                    allImages.addAll(allBackdropImgsFuture.get());
                    allImages.addAll(allPosterImages.get());

                    if (allImages.size() < 8) return false;
                    movie.addAllImages(allImages);
                }

            } catch (Exception exception) {
                exception.printStackTrace();
            }

            return true;
        }, asyncExecutor);
    }

    @Async
    public CompletableFuture<List<MovieImage>> extractDetailsImages(JsonArray backdropsJsonAr, ImageType imageType, int limit) {
        if (backdropsJsonAr == null || imageType == null) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<MovieImage> backdropImages = new ArrayList<>();

        int count = 0;
        for (JsonElement jsonElement : backdropsJsonAr) {
            MovieImage image = new MovieImage();

            if (imageType.equals(ImageType.BACKDROP)) image.setImageType(ImageType.BACKDROP);
            else image.setImageType(ImageType.POSTER);

            backdropImages.add(image.setImageURL(jsonElement.getAsJsonObject().get("file_path")
                    .getAsString()));

            if (count++ == limit) break;
        }

        return CompletableFuture.completedFuture(backdropImages);
    }

    public void saveMovie(String cinemaRecTitle, String cinemaRecPosterImage, Movie movie) {
        transactionTemplate.execute(status -> {
            Optional<Movie> cinemaRecResponse = this.movieRepository.findByTitleAndPosterImgURL(cinemaRecTitle, cinemaRecPosterImage);
            if (cinemaRecResponse.isEmpty()) {
                // "Присвояваме" актьорите към текущата сесия
                List<Actor> managedActors = new ArrayList<>();
                for (Actor actor : movie.getCastList()) {
                    if (actor.getId() != null) {
                        // Ако актьорът вече е в базата, зареждаме го отново
                        Actor managedActor = this.actorRepository.findById(actor.getId()).orElse(actor);
                        managedActors.add(managedActor);
                    } else {
                        // Ако няма ID, значи е нов – го запазваме, за да получим ID и управляван екземпляр
                        managedActors.add(this.actorRepository.save(actor));
                    }
                }
                movie.setCastList(managedActors);
                this.movieRepository.save(movie);
            }
            return null;
        });
    }

    public void addAllCast(List<Actor> allCast, Movie movie) {
        transactionTemplate.execute(status -> {
            int count = 0;

            for (Actor actor : allCast) {
                Optional<Actor> existingActor = this.actorRepository
                        .findByNameInRealLifeAndImageURL(actor.getNameInRealLife(), actor.getImageURL());

                if (existingActor.isPresent()) {
                    actor = existingActor.get();
                }

                // Добавяме връзката между актьора и филма
                if (!movie.getCastList().contains(actor)) {
                    movie.getCastList().add(actor);
                }

                // Добавяме филма към списъка на актьора
                if (!actor.getMoviesParticipations().contains(movie)) {
                    actor.getMoviesParticipations().add(movie);
                }

                if (count++ == 20) return true;
            }

            return true;
        });
    }
}
