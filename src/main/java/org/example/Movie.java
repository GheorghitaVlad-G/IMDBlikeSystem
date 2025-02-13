import utils.Genre;

import java.util.Objects;

public class Movie extends Production{
    private int releaseYear;
    private String movieLength;
    public Movie(String title, String plot, int releaseYear, String movieLength){
        super(title, plot, (double) 0);
        this.releaseYear = releaseYear;
        this.movieLength = movieLength;
    }
    public Movie(String title, String plot,Double averageRating, int releaseYear, String movieLength){
        super(title, plot, averageRating);
        this.releaseYear = releaseYear;
        this.movieLength = movieLength;
    }
    public void setReleaseYear(int x){
        this.releaseYear = x;
    }
    public void setMovieLength(String x){
        this.movieLength = x;
    }
    public int getReleaseYear(){
        return this.releaseYear;
    }
    public String getMovieLength(){
        return this.movieLength;
    }
    @Override
    public void displayInfo() {
        String formattedTitle;
        if(this.releaseYear > 0) {
            formattedTitle = this.getTitle() + " (" + this.releaseYear + ")";
        }
        else {
            formattedTitle = this.getTitle();
        }
        System.out.println("                     " + formattedTitle);
        System.out.println();
        System.out.print("Genres: ");
        for (Genre genre : this.getGenres()){
            if(Objects.equals(genre, this.getGenres().getLast())) System.out.print(genre.toString() + "\n");
            else System.out.print(genre.toString() + ", ");
        }
        System.out.println("Rating: " + this.getAverageRating());
        System.out.println("Length: " + this.movieLength);
        System.out.println("Plot: " + this.getPlot());
        System.out.print("Directors: ");
        for (String director : this.getDirectors()){
            if(Objects.equals(director, this.getDirectors().getLast())) System.out.print(director + "\n");
            else System.out.print(director + ", ");
        }
        System.out.print("Actors: ");
        for (String actor : this.getActors()){
            if(Objects.equals(actor, this.getActors().getLast())) System.out.print(actor + "\n");
            else System.out.print(actor + ", ");
        }
    }
    public String toString(){
        String str = "";
        str +=  super.toString();
        return str;
    }
}
